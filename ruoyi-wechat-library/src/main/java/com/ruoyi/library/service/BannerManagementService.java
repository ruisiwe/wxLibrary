package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.dto.BannerImagePreviewResult;
import com.ruoyi.library.storage.BannerImageProcessor;
import com.ruoyi.library.storage.CosPrivateStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 后台轮播图图片上传、元数据事务与 COS 对象补偿服务。 */
@Service
public class BannerManagementService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BannerManagementService.class);
    private static final Duration PREVIEW_URL_TTL = Duration.ofMinutes(30);

    private final BannerImageProcessor imageProcessor;
    private final CosPrivateStorageService storage;
    private final DocumentService documentService;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public BannerManagementService(BannerImageProcessor imageProcessor,
            CosPrivateStorageService storage, DocumentService documentService,
            PlatformTransactionManager transactionManager)
    {
        this.imageProcessor = imageProcessor;
        this.storage = storage;
        this.documentService = documentService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 上传本地图片并在数据库事务中新增轮播图。 */
    public int add(WlBanner banner, MultipartFile image, String operator)
    {
        requireOperator(operator);
        if (banner == null) throw new ServiceException("轮播图参数不能为空");
        if (image == null || image.isEmpty()) throw new ServiceException("轮播图图片不能为空");
        banner.setImageUrl(null);
        String newKey = uploadNewImage(image);
        banner.setImageUrl(newKey);
        try
        {
            Integer rows = transactionTemplate.execute(status ->
                    documentService.addBanner(banner, operator.trim()));
            if (rows == null || rows != 1) throw new ServiceException("轮播图保存失败，请重试");
            return rows;
        }
        catch (RuntimeException exception)
        {
            cleanupNewObject(newKey);
            throw safeMutationException(exception, "轮播图保存失败，请重试");
        }
    }

    /** 修改轮播图；没有新文件时保留数据库中的原图片对象键。 */
    public int update(WlBanner banner, MultipartFile image, String operator)
    {
        requireOperator(operator);
        if (banner == null || banner.getId() == null || banner.getId() <= 0)
            throw new ServiceException("轮播图编号不能为空");
        WlBanner current = documentService.getBanner(banner.getId());
        String oldKey = requireExistingImage(current.getImageUrl());
        if (image == null || image.isEmpty())
        {
            banner.setImageUrl(oldKey);
            try { return executeUpdate(banner, oldKey, operator.trim()); }
            catch (RuntimeException exception)
            {
                throw safeMutationException(exception, "轮播图保存失败，请重试");
            }
        }

        String newKey = uploadNewImage(image);
        banner.setImageUrl(newKey);
        int rows;
        try
        {
            rows = executeUpdate(banner, oldKey, operator.trim());
        }
        catch (RuntimeException exception)
        {
            cleanupNewObject(newKey);
            throw safeMutationException(exception, "轮播图保存失败，请重试");
        }
        cleanupOldObject(oldKey);
        return rows;
    }

    /** 在一个事务中删除所选轮播图，提交后再清理其私有图片。 */
    public int remove(Long[] ids, String operator)
    {
        requireOperator(operator);
        if (ids == null || ids.length == 0) throw new ServiceException("请选择要删除的轮播图");
        Set<Long> uniqueIds = new HashSet<>();
        List<WlBanner> current = new ArrayList<>();
        for (Long id : ids)
        {
            if (id == null || id <= 0) throw new ServiceException("轮播图编号不正确");
            if (!uniqueIds.add(id)) throw new ServiceException("轮播图编号不能重复");
            WlBanner banner = documentService.getBanner(id);
            requireExistingImage(banner.getImageUrl());
            current.add(banner);
        }

        Integer rows;
        try
        {
            rows = transactionTemplate.execute(status -> {
                int affected = 0;
                for (WlBanner banner : current)
                {
                    affected += documentService.removeBanner(
                            banner.getId(), banner.getImageUrl(), operator.trim());
                }
                return affected;
            });
        }
        catch (RuntimeException exception)
        {
            throw safeMutationException(exception, "轮播图删除失败，请重试");
        }
        if (rows == null || rows != current.size())
            throw new ServiceException("轮播图删除失败，请重试");
        for (WlBanner banner : current) cleanupOldObject(banner.getImageUrl());
        return rows;
    }

    /** 获取轮播图私有图片的短时预览地址。 */
    public BannerImagePreviewResult preview(Long id)
    {
        if (id == null || id <= 0) throw new ServiceException("轮播图编号不能为空");
        String imageUrl = requireExistingImage(documentService.getBanner(id).getImageUrl());
        if (isExternalUrl(imageUrl)) return new BannerImagePreviewResult(imageUrl);
        try
        {
            URL signed = storage.signGetUrl(imageUrl, PREVIEW_URL_TTL, null);
            if (signed == null) throw new ServiceException("轮播图图片服务暂不可用，请稍后重试");
            return new BannerImagePreviewResult(signed.toString());
        }
        catch (RuntimeException exception)
        {
            throw new ServiceException("轮播图图片服务暂不可用，请稍后重试");
        }
    }

    private int executeUpdate(WlBanner banner, String oldKey, String operator)
    {
        Integer rows = transactionTemplate.execute(status ->
                documentService.updateBanner(banner, oldKey, operator));
        if (rows == null || rows != 1) throw new ServiceException("轮播图保存失败，请重试");
        return rows;
    }

    private String uploadNewImage(MultipartFile image)
    {
        String objectKey = "banners/" + UUID.randomUUID().toString().replace("-", "") + "/image.jpg";
        BannerImageProcessor.ProcessedBannerImage processed = imageProcessor.process(image);
        boolean uploadAttempted = false;
        try (BannerImageProcessor.ProcessedBannerImage ignored = processed;
                InputStream input = processed.openStream())
        {
            uploadAttempted = true;
            storage.putPrivateObject(objectKey, input, processed.getSize(), processed.getContentType());
            return objectKey;
        }
        catch (IOException | RuntimeException exception)
        {
            if (uploadAttempted) cleanupNewObject(objectKey);
            throw new ServiceException("轮播图图片上传失败，请重试");
        }
    }

    private void cleanupNewObject(String objectKey)
    {
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException exception)
        {
            LOGGER.error("新轮播图图片补偿删除失败，对象键：{}", objectKey);
        }
    }

    private void cleanupOldObject(String objectKey)
    {
        if (objectKey == null || isExternalUrl(objectKey)) return;
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException exception)
        {
            LOGGER.warn("旧轮播图图片清理失败，对象键：{}", objectKey);
        }
    }

    private RuntimeException safeMutationException(RuntimeException exception, String fallback)
    {
        return exception instanceof ServiceException ? exception : new ServiceException(fallback);
    }

    private String requireExistingImage(String value)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException("轮播图原图片不存在");
        return value.trim();
    }

    private void requireOperator(String operator)
    {
        if (operator == null || operator.trim().isEmpty()) throw new ServiceException("操作人不能为空");
    }

    private boolean isExternalUrl(String value)
    {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }
}
