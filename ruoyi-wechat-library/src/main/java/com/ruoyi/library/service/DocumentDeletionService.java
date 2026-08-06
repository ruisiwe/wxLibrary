package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.storage.CosPrivateStorageService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** 后台文档逻辑删除与 COS 对象清理协调服务。 */
@Service
public class DocumentDeletionService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentDeletionService.class);

    private final DocumentService documentService;
    private final CosPrivateStorageService storage;
    private final TransactionTemplate transactionTemplate;

    public DocumentDeletionService(DocumentService documentService, CosPrivateStorageService storage,
            PlatformTransactionManager transactionManager)
    {
        this.documentService = documentService;
        this.storage = storage;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 逻辑删除文档，并在数据库删除成功后清理关联的私有对象。 */
    public int remove(Long[] ids, String operator)
    {
        requireIds(ids);
        DeletionSnapshot snapshot = transactionTemplate.execute(status -> {
            List<WlDocument> documents = documentService.lockDocumentsForDeletion(ids);
            int rows = documentService.removeDocuments(ids, operator);
            return new DeletionSnapshot(rows, documents);
        });
        if (snapshot == null) throw new ServiceException("文档删除失败，请重试");
        Set<String> objectKeys = new LinkedHashSet<>();
        for (WlDocument document : snapshot.documents)
        {
            addObjectKey(objectKeys, document.getOriginalObjectKey(), false);
            addObjectKey(objectKeys, document.getFullObjectKey(), false);
            addObjectKey(objectKeys, document.getPreviewObjectKey(), false);
            addObjectKey(objectKeys, document.getCoverUrl(), true);
        }
        for (String objectKey : objectKeys) cleanupObject(objectKey);
        return snapshot.rows;
    }

    private void addObjectKey(Set<String> objectKeys, String value, boolean skipExternalUrl)
    {
        if (value == null || value.trim().isEmpty()) return;
        String objectKey = value.trim();
        if (skipExternalUrl && isExternalUrl(objectKey)) return;
        objectKeys.add(objectKey);
    }

    private void cleanupObject(String objectKey)
    {
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException exception)
        {
            LOGGER.warn("文档云存储对象清理失败，对象键：{}", objectKey);
        }
    }

    private boolean isExternalUrl(String value)
    {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private void requireIds(Long[] ids)
    {
        if (ids == null || ids.length == 0) throw new ServiceException("请选择要操作的数据");
        for (Long id : ids)
        {
            if (id == null || id <= 0) throw new ServiceException("数据编号不正确");
        }
    }

    private static final class DeletionSnapshot
    {
        private final int rows;
        private final List<WlDocument> documents;

        private DeletionSnapshot(int rows, List<WlDocument> documents)
        {
            this.rows = rows;
            this.documents = new ArrayList<>(documents);
        }
    }
}
