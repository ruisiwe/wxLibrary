package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.domain.WlDocumentConversion;
import com.ruoyi.library.mapper.WlDocumentConversionMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.DocumentConverter;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 文档私有上传、转换、重试和上架状态服务。 */
@Service
public class DocumentConversionService
{
    private static final Set<String> FORMATS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("PDF", "DOC", "DOCX", "PPT", "PPTX", "TXT", "XLS", "XLSX")));
    private static final Map<String, Set<String>> CONTENT_TYPES = contentTypes();
    private static final byte[] OLE_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private static final Duration INTERNAL_SOURCE_TTL = Duration.ofMinutes(10);

    private final WlDocumentMapper documentMapper;
    private final WlDocumentConversionMapper conversionMapper;
    private final CosPrivateStorageService storage;
    private final DocumentConverter converter;
    private final DocumentService documentService;
    private final long maxOutputBytes;
    private final long maxInputBytes;
    private final TransactionTemplate transactionTemplate;

    DocumentConversionService(WlDocumentMapper documentMapper, WlDocumentConversionMapper conversionMapper,
            CosPrivateStorageService storage, DocumentConverter converter, DocumentService documentService,
            long maxOutputBytes)
    {
        this.documentMapper = documentMapper;
        this.conversionMapper = conversionMapper;
        this.storage = storage;
        this.converter = converter;
        this.documentService = documentService;
        this.maxOutputBytes = maxOutputBytes;
        this.maxInputBytes = maxOutputBytes;
        this.transactionTemplate = null;
    }

    @Autowired
    public DocumentConversionService(WlDocumentMapper documentMapper, WlDocumentConversionMapper conversionMapper,
            CosPrivateStorageService storage, DocumentConverter converter, DocumentService documentService,
            DocumentConversionProperties properties, PlatformTransactionManager transactionManager)
    {
        this.documentMapper = documentMapper;
        this.conversionMapper = conversionMapper;
        this.storage = storage;
        this.converter = converter;
        this.documentService = documentService;
        this.maxOutputBytes = properties.getMaxOutputBytes();
        this.maxInputBytes = properties.getMaxInputBytes();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 判断文件名扩展名是否属于明确允许的格式。 */
    public boolean supports(String fileName)
    {
        String extension = extensionOf(fileName);
        return extension != null && FORMATS.contains(extension);
    }

    /** 校验并上传原文件，创建持久化待转换任务。 */
    public WlDocumentConversion uploadOriginal(Long documentId, MultipartFile file, String operator)
    {
        WlDocument document = requireDraftDocument(documentId);
        String extension = requireSupportedExtension(file == null ? null : file.getOriginalFilename());
        if (document.getFileFormat() != null && !extension.equals(document.getFileFormat().trim().toUpperCase(Locale.ROOT)))
            throw new ServiceException("上传文件格式与文档元数据不一致");
        validateUpload(file, extension);
        int version = conversionMapper.selectNextVersion(documentId);
        if (version < 1) version = 1;
        String sourceKey = "documents/" + documentId + "/original/v" + version
                + "." + extension.toLowerCase(Locale.ROOT);
        try (InputStream input = file.getInputStream())
        {
            storage.putPrivateObject(sourceKey, input, file.getSize(), normalizeContentType(file.getContentType()));
        }
        catch (IOException exception)
        {
            throw new ServiceException("读取上传文档失败");
        }
        WlDocumentConversion task = new WlDocumentConversion();
        task.setDocumentId(documentId);
        task.setTaskVersion(version);
        task.setTaskStatus("PENDING");
        task.setSourceObjectKey(sourceKey);
        task.setCreateBy(operator == null ? "" : operator);
        try
        {
            executeInTransaction(() -> {
                if (conversionMapper.insertConversion(task) != 1
                        || documentMapper.updateConversionPending(documentId, sourceKey, extension,
                        file.getSize(), operator) != 1)
                    throw new ServiceException("文档转换任务创建失败，请重试");
                return null;
            });
        }
        catch (RuntimeException exception)
        {
            deleteQuietly(sourceKey);
            throw exception;
        }
        return task;
    }

    /** 执行一个待转换任务，失败原因会持久化并允许后续重试。 */
    public WlDocumentConversion processTask(Long taskId)
    {
        WlDocumentConversion task = requireTask(taskId);
        if (!"PENDING".equals(task.getTaskStatus()))
            throw new ServiceException("只有待转换任务可以执行");
        WlDocument document = requireDraftDocument(task.getDocumentId());
        executeInTransaction(() -> {
            if (conversionMapper.markConverting(taskId) != 1
                    || documentMapper.updateConversionStarted(task.getDocumentId(), "system") != 1)
                throw new ServiceException("文档转换任务状态已变化，请刷新后重试");
            return null;
        });
        task.setTaskStatus("CONVERTING");
        String fullKey = "documents/" + task.getDocumentId() + "/full/v" + task.getTaskVersion() + ".pdf";
        String previewKey = "documents/" + task.getDocumentId() + "/preview/v" + task.getTaskVersion() + ".pdf";
        boolean fullUploaded = false;
        boolean previewUploaded = false;
        try
        {
            URL sourceUrl = storage.signGetUrl(task.getSourceObjectKey(), INTERNAL_SOURCE_TTL, null);
            try (DocumentConverter.ConversionArtifacts artifacts = converter.convert(sourceUrl,
                    document.getFileFormat(), document.getPreviewPages(), maxOutputBytes))
            {
                validateArtifacts(artifacts, document.getPreviewPages());
                try (InputStream fullInput = Files.newInputStream(artifacts.getFullPdf()))
                {
                    storage.putPrivateObject(fullKey, fullInput, Files.size(artifacts.getFullPdf()), "application/pdf");
                    fullUploaded = true;
                }
                try (InputStream previewInput = Files.newInputStream(artifacts.getPreviewPdf()))
                {
                    storage.putPrivateObject(previewKey, previewInput,
                            Files.size(artifacts.getPreviewPdf()), "application/pdf");
                    previewUploaded = true;
                }
                int pages = artifacts.getPageCount();
                executeInTransaction(() -> {
                    if (conversionMapper.markSuccess(taskId, fullKey, previewKey, pages) != 1
                            || documentMapper.updateConversionSuccess(task.getDocumentId(), fullKey,
                            previewKey, pages, "system") != 1)
                        throw new ServiceException("文档转换结果保存失败，请重试");
                    return null;
                });
                task.setTaskStatus("SUCCESS");
                task.setFullObjectKey(fullKey);
                task.setPreviewObjectKey(previewKey);
                task.setPageCount(pages);
                task.setFailureReason(null);
                return task;
            }
        }
        catch (Exception exception)
        {
            if (fullUploaded) deleteQuietly(fullKey);
            if (previewUploaded) deleteQuietly(previewKey);
            String message = safeFailureMessage(exception);
            executeInTransaction(() -> {
                if (conversionMapper.markFailed(taskId, message) != 1
                        || documentMapper.updateConversionFailed(task.getDocumentId(), "system") != 1)
                    throw new ServiceException("文档转换失败状态保存失败，请重试");
                return null;
            });
            task.setTaskStatus("FAILED");
            task.setFailureReason(message);
            return task;
        }
    }

    /** 为失败任务创建新版本，保留旧任务审计记录。 */
    public WlDocumentConversion retry(Long taskId, String operator)
    {
        WlDocumentConversion failed = requireTask(taskId);
        if (!"FAILED".equals(failed.getTaskStatus())) throw new ServiceException("只有失败任务可以重试");
        requireDraftDocument(failed.getDocumentId());
        WlDocumentConversion retry = new WlDocumentConversion();
        retry.setDocumentId(failed.getDocumentId());
        retry.setTaskVersion(conversionMapper.selectNextVersion(failed.getDocumentId()));
        retry.setTaskStatus("PENDING");
        retry.setSourceObjectKey(failed.getSourceObjectKey());
        retry.setCreateBy(operator == null ? "" : operator);
        executeInTransaction(() -> {
            if (conversionMapper.insertConversion(retry) != 1
                    || documentMapper.updateConversionPending(failed.getDocumentId(), failed.getSourceObjectKey(),
                    null, null, operator) != 1)
                throw new ServiceException("文档转换重试任务创建失败，请重试");
            return null;
        });
        return retry;
    }

    public List<WlDocumentConversion> listTasks(WlDocumentConversion query)
    {
        return conversionMapper.selectList(query == null ? new WlDocumentConversion() : query);
    }

    public WlDocumentConversion getTask(Long id) { return requireTask(id); }

    /** 仅允许原文件、试看文件和缩略图均已生成且页数边界正确的文档上架。 */
    public int publishDocument(Long documentId, String operator)
    {
        WlDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null) throw new ServiceException("文档不存在");
        if (!"SUCCESS".equals(document.getConversionStatus()))
            throw new ServiceException("文档转换成功后才能上架");
        requireText(document.getOriginalObjectKey(), "原文件尚未上传");
        requireText(document.getPreviewObjectKey(), "试读文件尚未生成");
        requireText(document.getCoverUrl(), "缩略图尚未生成");
        if (document.getPageCount() == null || document.getPreviewPages() == null
                || document.getPreviewPages() <= 0 || document.getPreviewPages() >= document.getPageCount())
            throw new ServiceException("试读页数必须大于0且小于文档总页数");
        return documentService.publishDocument(documentId, operator);
    }

    private void validateUpload(MultipartFile file, String extension)
    {
        if (file == null || file.isEmpty() || file.getSize() < 1) throw new ServiceException("请选择要上传的文档");
        if (maxInputBytes < 1 || file.getSize() > maxInputBytes) throw new ServiceException("上传文档超过大小限制");
        String contentType = normalizeContentType(file.getContentType());
        if (!CONTENT_TYPES.get(extension).contains(contentType)) throw new ServiceException("文档 MIME 类型不正确");
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream()))
        {
            if (!matchesContent(input, extension)) throw new ServiceException("文件内容与文档格式不匹配");
        }
        catch (IOException exception)
        {
            throw new ServiceException("读取上传文档失败");
        }
    }

    private boolean matchesContent(BufferedInputStream input, String extension) throws IOException
    {
        input.mark(128 * 1024);
        byte[] header = new byte[8];
        int read = input.read(header);
        input.reset();
        if ("PDF".equals(extension)) return read >= 5 && header[0] == '%' && header[1] == 'P'
                && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
        if (Arrays.asList("DOC", "PPT", "XLS").contains(extension))
            return read == 8 && Arrays.equals(header, OLE_MAGIC);
        if ("DOCX".equals(extension) || "PPTX".equals(extension) || "XLSX".equals(extension))
            return matchesOpenXml(input, extension);
        if ("TXT".equals(extension)) return matchesUtf8Text(input);
        return false;
    }

    private boolean matchesOpenXml(InputStream input, String extension) throws IOException
    {
        boolean contentTypes = false;
        boolean documentEntry = false;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && entries++ < 4096)
            {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) contentTypes = true;
                if ("DOCX".equals(extension) && "word/document.xml".equals(name)) documentEntry = true;
                if ("PPTX".equals(extension) && "ppt/presentation.xml".equals(name)) documentEntry = true;
                if ("XLSX".equals(extension) && "xl/workbook.xml".equals(name)) documentEntry = true;
                if (contentTypes && documentEntry) return true;
            }
        }
        return false;
    }

    private boolean matchesUtf8Text(InputStream input) throws IOException
    {
        byte[] sample = new byte[64 * 1024];
        int length = input.read(sample);
        if (length < 1) return false;
        for (int index = 0; index < length; index++) if (sample[index] == 0) return false;
        java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        return !decoder.decode(ByteBuffer.wrap(sample, 0, length), CharBuffer.allocate(length), false).isError();
    }

    private void validateArtifacts(DocumentConverter.ConversionArtifacts artifacts, Integer previewPages)
            throws IOException
    {
        if (artifacts == null || artifacts.getFullPdf() == null || artifacts.getPreviewPdf() == null)
            throw new ServiceException("文档转换未生成完整结果");
        if (previewPages == null || previewPages <= 0 || previewPages >= artifacts.getPageCount())
            throw new ServiceException("试读页数必须大于0且小于文档总页数");
        if (!Files.isRegularFile(artifacts.getFullPdf()) || !Files.isRegularFile(artifacts.getPreviewPdf())
                || Files.size(artifacts.getFullPdf()) < 1 || Files.size(artifacts.getPreviewPdf()) < 1)
            throw new ServiceException("文档转换结果文件为空");
        if (Files.size(artifacts.getFullPdf()) > maxOutputBytes
                || Files.size(artifacts.getPreviewPdf()) > maxOutputBytes)
            throw new ServiceException("文档转换结果超过大小限制");
    }

    private WlDocument requireDraftDocument(Long documentId)
    {
        if (documentId == null || documentId <= 0) throw new ServiceException("文档编号不正确");
        WlDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null) throw new ServiceException("文档不存在");
        if (!"DRAFT".equals(document.getPublishStatus())) throw new ServiceException("请先下架文档后再处理文件");
        return document;
    }

    private WlDocumentConversion requireTask(Long id)
    {
        if (id == null || id <= 0) throw new ServiceException("文档转换任务编号不正确");
        WlDocumentConversion task = conversionMapper.selectById(id);
        if (task == null) throw new ServiceException("文档转换任务不存在");
        return task;
    }

    private String requireSupportedExtension(String fileName)
    {
        String extension = extensionOf(fileName);
        if (extension == null || !FORMATS.contains(extension)) throw new ServiceException("文档格式不支持");
        return extension;
    }

    private String extensionOf(String fileName)
    {
        if (fileName == null) return null;
        String name = fileName.trim();
        int index = name.lastIndexOf('.');
        if (index <= 0 || index == name.length() - 1) return null;
        String extension = name.substring(index + 1).toUpperCase(Locale.ROOT);
        return extension.matches("[A-Z0-9]{1,8}") ? extension : null;
    }

    private String normalizeContentType(String contentType)
    {
        if (contentType == null) return "";
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
    }

    private String safeFailureMessage(Exception exception)
    {
        String message = exception instanceof ServiceException ? exception.getMessage() : "文档转换失败，请重试";
        if (message == null || message.trim().isEmpty()) message = "文档转换失败，请重试";
        message = message.replaceAll("[\\r\\n\\t]", " ").trim();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private <T> T executeInTransaction(Supplier<T> callback)
    {
        return transactionTemplate == null ? callback.get() : transactionTemplate.execute(status -> callback.get());
    }

    private void deleteQuietly(String objectKey)
    {
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException ignored) { }
    }

    private void requireText(String value, String message)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(message);
    }

    private static Map<String, Set<String>> contentTypes()
    {
        Map<String, Set<String>> values = new HashMap<>();
        values.put("PDF", Collections.singleton("application/pdf"));
        values.put("DOC", Collections.singleton("application/msword"));
        values.put("DOCX", Collections.singleton(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        values.put("PPT", Collections.singleton("application/vnd.ms-powerpoint"));
        values.put("PPTX", Collections.singleton(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        values.put("TXT", Collections.singleton("text/plain"));
        values.put("XLS", Collections.singleton("application/vnd.ms-excel"));
        values.put("XLSX", Collections.singleton(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return Collections.unmodifiableMap(values);
    }
}
