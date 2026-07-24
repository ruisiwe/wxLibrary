package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.dto.DocumentUploadCommitRequest;
import com.ruoyi.library.dto.DocumentUploadCommitResult;
import com.ruoyi.library.dto.DocumentUploadPrepareResult;
import com.ruoyi.library.dto.DocumentThumbnailResult;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.PreparedDocumentProcessor;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 后台文档预处理临时会话与最终 COS 保存服务。 */
@Service
public class DocumentUploadService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentUploadService.class);
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration THUMBNAIL_URL_TTL = Duration.ofMinutes(10);
    private static final long MAX_CUSTOM_THUMBNAIL_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_CUSTOM_THUMBNAIL_PIXELS = 20_000_000L;
    private static final int THUMBNAIL_LONGEST_SIDE = 800;
    private static final String SESSION_DIRECTORY_PATTERN = "wl-upload-[a-f0-9]{32}";
    private static final Set<String> FORMATS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("PDF", "DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX", "TXT")));
    private static final Map<String, Set<String>> CONTENT_TYPES = contentTypes();
    private static final byte[] OLE_MAGIC = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};

    private final PreparedDocumentProcessor processor;
    private final CosPrivateStorageService storage;
    private final DocumentService documentService;
    private final DocumentConversionProperties properties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, UploadSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public DocumentUploadService(PreparedDocumentProcessor processor, CosPrivateStorageService storage,
            DocumentService documentService, DocumentConversionProperties properties,
            PlatformTransactionManager transactionManager)
    {
        this(processor, storage, documentService, properties, Clock.systemDefaultZone(),
                new TransactionTemplate(transactionManager));
    }

    DocumentUploadService(PreparedDocumentProcessor processor, CosPrivateStorageService storage,
            DocumentService documentService, DocumentConversionProperties properties, Clock clock)
    {
        this(processor, storage, documentService, properties, clock, null);
    }

    private DocumentUploadService(PreparedDocumentProcessor processor, CosPrivateStorageService storage,
            DocumentService documentService, DocumentConversionProperties properties, Clock clock,
            TransactionTemplate transactionTemplate)
    {
        this.processor = processor;
        this.storage = storage;
        this.documentService = documentService;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    /** 启动后清理上次进程遗留且已过期的上传目录。 */
    @PostConstruct
    public void cleanupAfterStartup()
    {
        cleanupOrphanDirectories();
    }

    /** 上传原文件并同步生成试看 PDF 和首页缩略图。 */
    public DocumentUploadPrepareResult prepare(MultipartFile file, String owner)
    {
        requireOwner(owner);
        UploadFileInfo fileInfo = validateDocumentFile(file);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        Path directory = createSessionDirectory(sessionId);
        Path original = directory.resolve("original." + fileInfo.extension.toLowerCase(Locale.ROOT));
        try
        {
            copyBounded(file, original, properties.getMaxInputBytes());
            PreparedDocumentProcessor.PreparedDocument prepared = processor.prepare(original,
                    fileInfo.extension, directory, properties.getMaxOutputBytes());
            Instant expiresAt = clock.instant().plus(SESSION_TTL);
            UploadSession session = new UploadSession(sessionId, owner.trim(), fileInfo.originalFileName,
                    fileInfo.extension, fileInfo.contentType, file.getSize(), prepared.getPageCount(),
                    prepared.getPreviewPages(), expiresAt, directory, original,
                    prepared.getPreviewPdf(), prepared.getThumbnail());
            sessions.put(sessionId, session);
            return toPrepareResult(session);
        }
        catch (RuntimeException exception)
        {
            deleteDirectoryQuietly(directory);
            throw exception;
        }
    }

    /** 读取当前管理用户会话中的缩略图文件。 */
    public Path thumbnail(String sessionId, String owner)
    {
        UploadSession session = requireSession(sessionId, owner);
        requireSafeSessionFile(session.thumbnail, session.directory);
        return session.thumbnail;
    }

    /** 替换当前会话缩略图并归一化为 JPG。 */
    public DocumentUploadPrepareResult replaceThumbnail(String sessionId, MultipartFile file, String owner)
    {
        UploadSession session = requireSession(sessionId, owner);
        synchronized (session)
        {
            requireActiveSession(session);
            return replaceThumbnailLocked(session, file);
        }
    }

    private DocumentUploadPrepareResult replaceThumbnailLocked(UploadSession session, MultipartFile file)
    {
        Path candidate = session.directory.resolve("thumbnail-new-"
                + UUID.randomUUID().toString().replace("-", "") + ".jpg");
        try
        {
            BufferedImage source = readValidatedThumbnail(file);
            BufferedImage normalized = normalizeThumbnail(source);
            writeJpeg(candidate, normalized);
            replaceAtomically(candidate, session.thumbnail);
            return toPrepareResult(session);
        }
        catch (ServiceException exception) { throw exception; }
        catch (Exception exception) { throw new ServiceException("缩略图处理失败，请重试"); }
        finally { deleteFileQuietly(candidate); }
    }

    /** 获取已保存文档私有缩略图的短时访问地址。 */
    public DocumentThumbnailResult savedThumbnail(Long documentId, String owner)
    {
        requireOwner(owner);
        if (documentId == null || documentId <= 0) throw new ServiceException("文档编号不能为空");
        WlDocument document = documentService.getDocument(documentId);
        String coverKey = document.getCoverUrl();
        DocumentThumbnailResult result = new DocumentThumbnailResult();
        result.setDocumentId(documentId);
        if (coverKey == null || coverKey.trim().isEmpty()) return result;
        String normalizedCoverKey = coverKey.trim();
        if (isExternalUrl(normalizedCoverKey))
        {
            result.setThumbnailUrl(normalizedCoverKey);
            return result;
        }
        requireDocumentThumbnailKey(documentId, normalizedCoverKey);
        result.setThumbnailUrl(storage.signGetUrl(
                normalizedCoverKey, THUMBNAIL_URL_TTL, null).toString());
        return result;
    }

    /** 替换已保存文档的私有缩略图，并在数据库提交后清理旧对象。 */
    public DocumentThumbnailResult replaceSavedThumbnail(Long documentId, MultipartFile file, String owner)
    {
        requireOwner(owner);
        if (documentId == null || documentId <= 0) throw new ServiceException("文档编号不能为空");
        WlDocument document = documentService.getDocument(documentId);
        String oldCoverKey = document.getCoverUrl();
        if (oldCoverKey == null || oldCoverKey.trim().isEmpty())
            throw new ServiceException("文档原缩略图不存在");
        if (!isExternalUrl(oldCoverKey)) requireDocumentThumbnailKey(documentId, oldCoverKey.trim());
        Path directory = createSessionDirectory(UUID.randomUUID().toString().replace("-", ""));
        Path thumbnail = directory.resolve("thumbnail.jpg");
        String newCoverKey = "documents/" + documentId + "/thumbnail/v"
                + UUID.randomUUID().toString().replace("-", "") + ".jpg";
        boolean uploaded = false;
        try
        {
            writeJpeg(thumbnail, normalizeThumbnail(readValidatedThumbnail(file)));
            upload(thumbnail, newCoverKey, "image/jpeg");
            uploaded = true;
            URL signedThumbnail = storage.signGetUrl(newCoverKey, THUMBNAIL_URL_TTL, null);
            executeInTransaction(() -> {
                documentService.updateDocumentCover(documentId, oldCoverKey, newCoverKey, owner);
                return null;
            });
            if (!isExternalUrl(oldCoverKey))
            {
                try { storage.deleteObjectAfterMetadataDeletion(oldCoverKey); }
                catch (RuntimeException exception)
                {
                    LOGGER.warn("旧缩略图清理失败，对象键：{}", oldCoverKey);
                }
            }
            DocumentThumbnailResult result = new DocumentThumbnailResult();
            result.setDocumentId(documentId);
            result.setThumbnailUrl(signedThumbnail.toString());
            return result;
        }
        catch (RuntimeException exception)
        {
            if (uploaded)
            {
                try { storage.deleteObjectAfterMetadataDeletion(newCoverKey); }
                catch (RuntimeException cleanupException)
                {
                    LOGGER.error("新缩略图补偿删除失败，对象键：{}", newCoverKey);
                }
            }
            if (exception instanceof ServiceException) throw exception;
            throw new ServiceException("缩略图替换失败，请重试");
        }
        finally { deleteDirectoryQuietly(directory); }
    }

    /** 取消并清理未提交的临时会话。 */
    public void cancel(String sessionId, String owner)
    {
        UploadSession session = requireSession(sessionId, owner);
        synchronized (session)
        {
            requireActiveSession(session);
            session.cancelled = true;
            if (deleteDirectoryQuietly(session.directory)) sessions.remove(sessionId, session);
        }
    }

    /** 上传三个最终对象并在数据库事务中新增文档。 */
    public DocumentUploadCommitResult commit(String sessionId, DocumentUploadCommitRequest request, String owner)
    {
        UploadSession session = requireSession(sessionId, owner);
        synchronized (session)
        {
            requireActiveSession(session);
            return commitLocked(session, request, owner);
        }
    }

    private DocumentUploadCommitResult commitLocked(UploadSession session,
            DocumentUploadCommitRequest request, String owner)
    {
        requireCommitRequest(request);
        String prefix = "documents/" + session.sessionId;
        String originalKey = prefix + "/original/v1." + session.extension.toLowerCase(Locale.ROOT);
        String previewKey = prefix + "/preview/v1.pdf";
        String thumbnailKey = prefix + "/thumbnail/v1.jpg";
        List<String> attemptedObjects = new ArrayList<>();
        try
        {
            attemptedObjects.add(originalKey);
            upload(session.original, originalKey, session.contentType);
            attemptedObjects.add(previewKey);
            upload(session.previewPdf, previewKey, "application/pdf");
            attemptedObjects.add(thumbnailKey);
            upload(session.thumbnail, thumbnailKey, "image/jpeg");
            URL signedThumbnail = storage.signGetUrl(thumbnailKey, THUMBNAIL_URL_TTL, null);
            WlDocument document = buildDocument(request, session, originalKey, previewKey, thumbnailKey);
            executeInTransaction(() -> {
                if (documentService.addProcessedDocument(document, owner) != 1)
                    throw new ServiceException("文档保存失败，请重试");
                return null;
            });
            sessions.remove(session.sessionId, session);
            deleteDirectoryQuietly(session.directory);
            DocumentUploadCommitResult result = new DocumentUploadCommitResult();
            result.setDocumentId(document.getId());
            result.setConversionStatus("SUCCESS");
            result.setThumbnailUrl(signedThumbnail.toString());
            return result;
        }
        catch (RuntimeException exception)
        {
            compensate(attemptedObjects);
            if (exception instanceof ServiceException
                    && "文档保存失败，请重试".equals(exception.getMessage())) throw exception;
            throw new ServiceException("文件保存失败，请重试");
        }
    }

    private void requireActiveSession(UploadSession session)
    {
        if (session == null || session.cancelled || sessions.get(session.sessionId) != session)
            throw new ServiceException("临时文件不存在或已过期");
    }

    /** 定时清理已过期且未提交的上传会话。 */
    @Scheduled(initialDelay = 60000L, fixedDelay = 300000L)
    public void cleanupExpired()
    {
        Instant now = clock.instant();
        for (UploadSession session : sessions.values())
        {
            if (session.cancelled || !now.isBefore(session.expiresAt))
            {
                synchronized (session)
                {
                    if (sessions.get(session.sessionId) == session
                            && deleteDirectoryQuietly(session.directory))
                        sessions.remove(session.sessionId, session);
                }
            }
        }
        cleanupOrphanDirectories();
    }

    private WlDocument buildDocument(DocumentUploadCommitRequest request, UploadSession session,
            String originalKey, String previewKey, String thumbnailKey)
    {
        WlDocument document = new WlDocument();
        document.setCategoryId(request.getCategoryId());
        document.setTitle(request.getTitle());
        document.setSummary(request.getSummary());
        document.setTags(request.getTags());
        document.setPointPrice(request.getPointPrice());
        document.setAccessType(request.getAccessType());
        document.setSortOrder(request.getSortOrder());
        document.setRemark(request.getRemark());
        document.setFileFormat(session.extension);
        document.setFileSize(session.fileSize);
        document.setPageCount(session.pageCount);
        document.setPreviewPages(session.previewPages);
        document.setOriginalObjectKey(originalKey);
        document.setFullObjectKey(null);
        document.setPreviewObjectKey(previewKey);
        document.setCoverUrl(thumbnailKey);
        return document;
    }

    private void upload(Path file, String objectKey, String contentType)
    {
        requireSafeSessionFile(file, file.getParent());
        try (InputStream input = Files.newInputStream(file))
        {
            storage.putPrivateObject(objectKey, input, Files.size(file), contentType);
        }
        catch (IOException exception) { throw new ServiceException("读取临时文件失败，请重新处理"); }
    }

    private void compensate(List<String> uploaded)
    {
        for (int index = uploaded.size() - 1; index >= 0; index--)
        {
            try { storage.deleteObjectAfterMetadataDeletion(uploaded.get(index)); }
            catch (RuntimeException exception)
            {
                LOGGER.error("文档上传补偿删除失败，对象键：{}", uploaded.get(index));
            }
        }
    }

    private UploadSession requireSession(String sessionId, String owner)
    {
        if (sessionId == null || !sessionId.matches("[a-f0-9]{32}"))
            throw new ServiceException("临时文件不存在或已过期");
        requireOwner(owner);
        UploadSession session = sessions.get(sessionId);
        if (session == null) throw new ServiceException("临时文件不存在或已过期");
        if (!session.owner.equals(owner.trim())) throw new ServiceException("临时文件无权访问");
        if (session.cancelled) throw new ServiceException("临时文件不存在或已过期");
        if (!clock.instant().isBefore(session.expiresAt))
        {
            synchronized (session)
            {
                if (sessions.get(sessionId) == session && deleteDirectoryQuietly(session.directory))
                    sessions.remove(sessionId, session);
            }
            throw new ServiceException("临时文件已过期，请重新处理");
        }
        requireSafeSessionDirectory(session.directory);
        return session;
    }

    private Path createSessionDirectory(String sessionId)
    {
        try
        {
            Path uploadRoot = resolveUploadRoot(true);
            Path directory = uploadRoot.resolve("wl-upload-" + sessionId).normalize();
            if (!directory.getParent().equals(uploadRoot)
                    || !directory.getFileName().toString().matches(SESSION_DIRECTORY_PATTERN))
                throw new ServiceException("文档临时目录不安全");
            Files.createDirectory(directory);
            return directory;
        }
        catch (ServiceException exception) { throw exception; }
        catch (IOException exception) { throw new ServiceException("无法创建文档临时目录"); }
    }

    private UploadFileInfo validateDocumentFile(MultipartFile file)
    {
        if (file == null || file.isEmpty() || file.getSize() < 1) throw new ServiceException("请选择要上传的文档");
        if (properties.getMaxInputBytes() < 1 || file.getSize() > properties.getMaxInputBytes())
            throw new ServiceException("上传文档超过大小限制");
        String originalFileName = safeOriginalFileName(file.getOriginalFilename());
        String extension = extensionOf(originalFileName);
        if (extension == null || !FORMATS.contains(extension)) throw new ServiceException("文件格式不支持");
        String contentType = normalizeContentType(file.getContentType());
        if (!CONTENT_TYPES.get(extension).contains(contentType)) throw new ServiceException("文件 MIME 类型不正确");
        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream()))
        {
            if (!matchesContent(input, extension)) throw new ServiceException("文件内容与扩展名不一致");
        }
        catch (IOException exception) { throw new ServiceException("读取上传文档失败"); }
        return new UploadFileInfo(originalFileName, extension, contentType);
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
        if (Arrays.asList("DOCX", "PPTX", "XLSX").contains(extension))
            return matchesOpenXml(input, extension);
        if ("TXT".equals(extension)) return matchesUtf8Text(input);
        return false;
    }

    private boolean matchesOpenXml(InputStream input, String extension) throws IOException
    {
        boolean contentTypes = false;
        boolean formatEntry = false;
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(input))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null && entries++ < 4096)
            {
                String name = entry.getName();
                if ("[Content_Types].xml".equals(name)) contentTypes = true;
                if ("DOCX".equals(extension) && "word/document.xml".equals(name)) formatEntry = true;
                if ("PPTX".equals(extension) && "ppt/presentation.xml".equals(name)) formatEntry = true;
                if ("XLSX".equals(extension) && "xl/workbook.xml".equals(name)) formatEntry = true;
                if (contentTypes && formatEntry) return true;
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

    private BufferedImage readValidatedThumbnail(MultipartFile file)
    {
        if (file == null || file.isEmpty() || file.getSize() < 1) throw new ServiceException("请选择缩略图");
        if (file.getSize() > MAX_CUSTOM_THUMBNAIL_BYTES) throw new ServiceException("缩略图不能超过5MB");
        String extension = extensionOf(safeOriginalFileName(file.getOriginalFilename()));
        if (!Arrays.asList("JPG", "JPEG", "PNG").contains(extension))
            throw new ServiceException("缩略图格式不支持");
        String type = normalizeContentType(file.getContentType());
        if (!Arrays.asList("image/jpeg", "image/png").contains(type))
            throw new ServiceException("缩略图格式不支持");
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(file.getInputStream()))
        {
            if (imageInput == null) throw new ServiceException("缩略图内容无法识别");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new ServiceException("缩略图内容无法识别");
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(imageInput, true, true);
                String actualFormat = normalizeImageFormat(reader.getFormatName());
                if (!matchesImageDeclaration(extension, type, actualFormat))
                    throw new ServiceException("缩略图扩展名、MIME 类型与实际格式不一致");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width < 1 || height < 1 || pixels > MAX_CUSTOM_THUMBNAIL_PIXELS)
                    throw new ServiceException("缩略图像素不能超过2000万");
                BufferedImage image = reader.read(0);
                if (image == null) throw new ServiceException("缩略图内容无法识别");
                return image;
            }
            finally { reader.dispose(); }
        }
        catch (ServiceException exception) { throw exception; }
        catch (IOException exception) { throw new ServiceException("缩略图内容无法识别"); }
    }

    private String normalizeImageFormat(String format)
    {
        if (format == null) return "";
        String normalized = format.trim().toUpperCase(Locale.ROOT);
        return "JPG".equals(normalized) ? "JPEG" : normalized;
    }

    private boolean matchesImageDeclaration(String extension, String type, String actualFormat)
    {
        String declaredFormat = "JPG".equals(extension) ? "JPEG" : extension;
        String mimeFormat = "image/jpeg".equals(type) ? "JPEG" : "PNG";
        return declaredFormat.equals(actualFormat) && mimeFormat.equals(actualFormat);
    }

    private void writeJpeg(Path target, BufferedImage image)
    {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(target))
            throw new ServiceException("缩略图临时文件不安全");
        try (OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE))
        {
            if (!ImageIO.write(image, "jpg", output)) throw new ServiceException("缩略图处理失败，请重试");
        }
        catch (ServiceException exception) { throw exception; }
        catch (IOException exception) { throw new ServiceException("缩略图处理失败，请重试"); }
    }

    private void replaceAtomically(Path source, Path target) throws IOException
    {
        try
        {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException exception)
        {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private BufferedImage normalizeThumbnail(BufferedImage source)
    {
        double scale = Math.min(1D, (double) THUMBNAIL_LONGEST_SIDE
                / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, width, height, null);
        }
        finally { graphics.dispose(); }
        return target;
    }

    private void copyBounded(MultipartFile file, Path target, long maxBytes)
    {
        try (InputStream input = file.getInputStream(); OutputStream output = Files.newOutputStream(target))
        {
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = input.read(buffer)) != -1)
            {
                total += read;
                if (maxBytes < 1 || total > maxBytes) throw new ServiceException("上传文档超过大小限制");
                output.write(buffer, 0, read);
            }
        }
        catch (ServiceException exception) { throw exception; }
        catch (IOException exception) { throw new ServiceException("保存上传文档失败，请重试"); }
    }

    private void requireSafeSessionDirectory(Path directory)
    {
        try
        {
            Path uploadRoot = resolveUploadRoot(false);
            Path normalized = directory == null ? null : directory.toAbsolutePath().normalize();
            if (uploadRoot == null || normalized == null || normalized.getFileName() == null
                    || !normalized.getFileName().toString().matches(SESSION_DIRECTORY_PATTERN)
                    || !normalized.getParent().equals(uploadRoot)
                    || !Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(normalized))
                throw new ServiceException("文档临时目录不安全");
        }
        catch (IOException exception)
        {
            throw new ServiceException("文档临时目录不安全");
        }
    }

    private void requireSafeSessionFile(Path file, Path directory)
    {
        requireSafeSessionDirectory(directory);
        if (file == null || !file.toAbsolutePath().normalize().getParent().equals(directory.toAbsolutePath().normalize())
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file))
            throw new ServiceException("上传临时文件不安全");
    }

    private DocumentUploadPrepareResult toPrepareResult(UploadSession session)
    {
        DocumentUploadPrepareResult result = new DocumentUploadPrepareResult();
        result.setSessionId(session.sessionId);
        result.setOriginalFileName(session.originalFileName);
        result.setFileFormat(session.extension);
        result.setFileSize(session.fileSize);
        result.setPageCount(session.pageCount);
        result.setPreviewPages(session.previewPages);
        result.setThumbnailUrl("/library/document-upload/session/" + session.sessionId + "/thumbnail");
        ZoneOffset offset = clock.getZone().getRules().getOffset(session.expiresAt);
        result.setExpiresAt(OffsetDateTime.ofInstant(session.expiresAt, offset).toString());
        return result;
    }

    private void requireCommitRequest(DocumentUploadCommitRequest request)
    {
        if (request == null) throw new ServiceException("文档参数不能为空");
        if (request.getCategoryId() == null || request.getCategoryId() <= 0) throw new ServiceException("文档分类不能为空");
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) throw new ServiceException("文档标题不能为空");
        if (request.getPointPrice() == null || request.getPointPrice() < 0) throw new ServiceException("兑换积分不能小于0");
        String accessType = request.getAccessType();
        if (accessType != null && !accessType.trim().isEmpty()
                && !"POINT".equals(accessType.trim().toUpperCase(Locale.ROOT))
                && !"VIP_FREE".equals(accessType.trim().toUpperCase(Locale.ROOT)))
            throw new ServiceException("文档访问方式不正确");
    }

    private void requireOwner(String owner)
    {
        if (owner == null || owner.trim().isEmpty()) throw new ServiceException("管理用户身份无效");
    }

    private String safeOriginalFileName(String value)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException("文件名不能为空");
        String normalized = value.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (name.isEmpty() || name.length() > 255) throw new ServiceException("文件名不正确");
        return name;
    }

    private String extensionOf(String fileName)
    {
        if (fileName == null) return null;
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index == fileName.length() - 1) return null;
        String extension = fileName.substring(index + 1).toUpperCase(Locale.ROOT);
        return extension.matches("[A-Z0-9]{1,8}") ? extension : null;
    }

    private String normalizeContentType(String value)
    {
        if (value == null) return "";
        int separator = value.indexOf(';');
        return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
    }

    private <T> T executeInTransaction(Supplier<T> callback)
    {
        return transactionTemplate == null ? callback.get() : transactionTemplate.execute(status -> callback.get());
    }

    private Path configuredTempRoot()
    {
        Path configuredRoot = properties.getTempDirectory() == null
                || properties.getTempDirectory().trim().isEmpty()
                ? Paths.get(System.getProperty("java.io.tmpdir"), "wechat-library-conversion")
                : Paths.get(properties.getTempDirectory().trim());
        return configuredRoot.toAbsolutePath().normalize();
    }

    private Path resolveUploadRoot(boolean create) throws IOException
    {
        Path root = configuredTempRoot();
        if (create && !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(root);
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
            throw new ServiceException("文档临时目录不安全");
        Path uploadRoot = root.resolve("upload-sessions").normalize();
        if (!uploadRoot.getParent().equals(root)) throw new ServiceException("文档临时目录不安全");
        if (create && !Files.exists(uploadRoot, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(uploadRoot);
        if (!Files.exists(uploadRoot, LinkOption.NOFOLLOW_LINKS)) return null;
        if (Files.isSymbolicLink(uploadRoot) || !Files.isDirectory(uploadRoot, LinkOption.NOFOLLOW_LINKS))
            throw new ServiceException("文档临时目录不安全");
        return uploadRoot;
    }

    private void cleanupOrphanDirectories()
    {
        try
        {
            Path uploadRoot = resolveUploadRoot(false);
            if (uploadRoot == null) return;
            Instant threshold = clock.instant().minus(SESSION_TTL);
            try (Stream<Path> paths = Files.list(uploadRoot))
            {
                paths.filter(path -> path.getFileName() != null
                                && path.getFileName().toString().matches(SESSION_DIRECTORY_PATTERN))
                        .filter(path -> !Files.isSymbolicLink(path))
                        .filter(path -> !sessions.containsKey(path.getFileName().toString().substring(10)))
                        .filter(path -> isOlderThan(path, threshold))
                        .forEach(this::deleteDirectoryQuietly);
            }
        }
        catch (RuntimeException | IOException exception)
        {
            LOGGER.warn("文档上传遗留目录扫描失败，将在下次清理时重试");
        }
    }

    private boolean isOlderThan(Path path, Instant threshold)
    {
        try { return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(threshold); }
        catch (IOException exception) { return false; }
    }

    private boolean deleteDirectoryQuietly(Path directory)
    {
        try
        {
            Path uploadRoot = resolveUploadRoot(false);
            Path normalized = directory == null ? null : directory.toAbsolutePath().normalize();
            if (uploadRoot == null || normalized == null || normalized.getFileName() == null
                    || !normalized.getFileName().toString().matches(SESSION_DIRECTORY_PATTERN)
                    || !uploadRoot.equals(normalized.getParent()) || Files.isSymbolicLink(normalized))
                return false;
            if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) return true;
            List<Path> candidates;
            try (Stream<Path> paths = Files.walk(normalized))
            {
                candidates = paths.sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
            }
            for (Path candidate : candidates) if (Files.isSymbolicLink(candidate)) return false;
            boolean deleted = true;
            for (Path candidate : candidates)
            {
                try { Files.deleteIfExists(candidate); }
                catch (IOException exception) { deleted = false; }
            }
            return deleted;
        }
        catch (RuntimeException | IOException exception)
        {
            LOGGER.warn("文档临时会话清理失败，会由过期清理重试");
            return false;
        }
    }

    private void deleteFileQuietly(Path path)
    {
        try { if (path != null && !Files.isSymbolicLink(path)) Files.deleteIfExists(path); }
        catch (IOException ignored) { }
    }

    private boolean isExternalUrl(String value)
    {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private void requireDocumentThumbnailKey(Long documentId, String objectKey)
    {
        String replacementPrefix = "documents/" + documentId + "/thumbnail/";
        boolean replacementKey = objectKey.startsWith(replacementPrefix)
                && objectKey.substring(replacementPrefix.length()).matches("v[a-f0-9]{32}\\.jpg");
        boolean legacyKey = objectKey.equals(replacementPrefix + "v1.jpg");
        boolean initialKey = objectKey.matches("documents/[a-f0-9]{32}/thumbnail/v1\\.jpg");
        if (!replacementKey && !legacyKey && !initialKey)
            throw new ServiceException("文档缩略图对象键不正确");
    }

    private static Map<String, Set<String>> contentTypes()
    {
        Map<String, Set<String>> values = new HashMap<>();
        values.put("PDF", Collections.singleton("application/pdf"));
        values.put("DOC", Collections.singleton("application/msword"));
        values.put("DOCX", Collections.singleton("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        values.put("PPT", Collections.singleton("application/vnd.ms-powerpoint"));
        values.put("PPTX", Collections.singleton("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        values.put("XLS", Collections.singleton("application/vnd.ms-excel"));
        values.put("XLSX", Collections.singleton("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        values.put("TXT", Collections.singleton("text/plain"));
        return Collections.unmodifiableMap(values);
    }

    private static final class UploadFileInfo
    {
        private final String originalFileName;
        private final String extension;
        private final String contentType;

        private UploadFileInfo(String originalFileName, String extension, String contentType)
        {
            this.originalFileName = originalFileName;
            this.extension = extension;
            this.contentType = contentType;
        }
    }

    private static final class UploadSession
    {
        private final String sessionId;
        private final String owner;
        private final String originalFileName;
        private final String extension;
        private final String contentType;
        private final long fileSize;
        private final int pageCount;
        private final int previewPages;
        private final Instant expiresAt;
        private final Path directory;
        private final Path original;
        private final Path previewPdf;
        private final Path thumbnail;
        private volatile boolean cancelled;

        private UploadSession(String sessionId, String owner, String originalFileName, String extension,
                String contentType, long fileSize, int pageCount, int previewPages, Instant expiresAt,
                Path directory, Path original, Path previewPdf, Path thumbnail)
        {
            this.sessionId = sessionId;
            this.owner = owner;
            this.originalFileName = originalFileName;
            this.extension = extension;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.pageCount = pageCount;
            this.previewPages = previewPages;
            this.expiresAt = expiresAt;
            this.directory = directory;
            this.original = original;
            this.previewPdf = previewPdf;
            this.thumbnail = thumbnail;
        }
    }
}
