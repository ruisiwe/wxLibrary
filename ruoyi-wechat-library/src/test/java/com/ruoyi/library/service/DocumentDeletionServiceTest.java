package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.storage.CosPrivateStorageService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DocumentDeletionServiceTest
{
    private DocumentService documentService;
    private CosPrivateStorageService storage;
    private RecordingTransactionManager transactionManager;
    private DocumentDeletionService service;

    @BeforeEach
    void setUp()
    {
        documentService = mock(DocumentService.class);
        storage = mock(CosPrivateStorageService.class);
        transactionManager = new RecordingTransactionManager();
        service = new DocumentDeletionService(documentService, storage, transactionManager);
    }

    @Test
    void deleteRemovesAllCosObjectsAfterDatabaseDeletion()
    {
        WlDocument document = storedDocument(7L);
        when(documentService.lockDocumentsForDeletion(any(Long[].class)))
                .thenReturn(Collections.singletonList(document));
        when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-object");
            return null;
        }).when(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");

        assertEquals(1, service.remove(new Long[] {7L}, "admin"));

        InOrder order = inOrder(documentService, storage);
        order.verify(documentService).lockDocumentsForDeletion(any(Long[].class));
        order.verify(documentService).removeDocuments(any(Long[].class), eq("admin"));
        order.verify(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                transactionManager.propagationBehavior);
        assertEquals(Arrays.asList("begin", "commit", "delete-object"), transactionManager.events);
        verify(storage).deleteObjectAfterMetadataDeletion("documents/7/full.pdf");
        verify(storage).deleteObjectAfterMetadataDeletion("documents/7/preview.pdf");
        verify(storage).deleteObjectAfterMetadataDeletion("documents/7/thumbnail.jpg");
    }

    @Test
    void databaseFailureDoesNotDeleteCosObjects()
    {
        when(documentService.lockDocumentsForDeletion(any(Long[].class)))
                .thenReturn(Collections.singletonList(storedDocument(7L)));
        when(documentService.removeDocuments(any(Long[].class), eq("admin")))
                .thenThrow(new ServiceException("文档状态已变化，请刷新后重试"));

        assertThrows(ServiceException.class, () -> service.remove(new Long[] {7L}, "admin"));

        verify(storage, never()).deleteObjectAfterMetadataDeletion(anyString());
    }

    @Test
    void storageFailureDoesNotStopRemainingCleanup()
    {
        when(documentService.lockDocumentsForDeletion(any(Long[].class)))
                .thenReturn(Collections.singletonList(storedDocument(7L)));
        when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(1);
        doThrow(new ServiceException("COS删除失败"))
                .when(storage).deleteObjectAfterMetadataDeletion("documents/7/original.pdf");

        assertEquals(1, service.remove(new Long[] {7L}, "admin"));

        verify(storage).deleteObjectAfterMetadataDeletion("documents/7/preview.pdf");
        verify(storage).deleteObjectAfterMetadataDeletion("documents/7/thumbnail.jpg");
    }

    @Test
    void deleteSkipsBlankDuplicateAndExternalKeys()
    {
        WlDocument first = storedDocument(7L);
        first.setFullObjectKey(" ");
        first.setPreviewObjectKey("documents/shared/original.pdf");
        first.setOriginalObjectKey("documents/shared/original.pdf");
        first.setCoverUrl("https://legacy.example/thumbnail.jpg");
        WlDocument second = storedDocument(8L);
        second.setOriginalObjectKey("documents/shared/original.pdf");
        second.setFullObjectKey(null);
        second.setPreviewObjectKey(" ");
        second.setCoverUrl("HTTPS://legacy.example/second.jpg");
        when(documentService.lockDocumentsForDeletion(any(Long[].class)))
                .thenReturn(Arrays.asList(first, second));
        when(documentService.removeDocuments(any(Long[].class), eq("admin"))).thenReturn(2);

        assertEquals(2, service.remove(new Long[] {7L, 8L}, "admin"));

        verify(storage).deleteObjectAfterMetadataDeletion("documents/shared/original.pdf");
        verify(storage, never()).deleteObjectAfterMetadataDeletion(
                "https://legacy.example/thumbnail.jpg");
        verifyNoMoreInteractions(storage);
    }

    private WlDocument storedDocument(Long id)
    {
        WlDocument document = new WlDocument();
        document.setId(id);
        document.setPublishStatus("DRAFT");
        document.setOriginalObjectKey("documents/" + id + "/original.pdf");
        document.setFullObjectKey("documents/" + id + "/full.pdf");
        document.setPreviewObjectKey("documents/" + id + "/preview.pdf");
        document.setCoverUrl("documents/" + id + "/thumbnail.jpg");
        return document;
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager
    {
        private final List<String> events = new ArrayList<>();
        private int propagationBehavior;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition)
        {
            events.add("begin");
            propagationBehavior = definition.getPropagationBehavior();
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status)
        {
            events.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status)
        {
            events.add("rollback");
        }
    }
}
