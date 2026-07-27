package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.mapper.WlVipPageConfigMapper;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.VipCustomerServiceImageProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** VIP 套餐页面权益和客服微信配置服务。 */
@Service
public class VipPageConfigService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VipPageConfigService.class);
    private static final Duration IMAGE_URL_TTL = Duration.ofMinutes(30);

    private final WlVipPageConfigMapper configMapper;
    private final VipBenefitService benefitService;
    private final VipCustomerServiceImageProcessor imageProcessor;
    private final CosPrivateStorageService storage;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VipPageConfigService(WlVipPageConfigMapper configMapper,
            VipBenefitService benefitService,
            VipCustomerServiceImageProcessor imageProcessor,
            CosPrivateStorageService storage,
            PlatformTransactionManager transactionManager)
    {
        this.configMapper = configMapper;
        this.benefitService = benefitService;
        this.imageProcessor = imageProcessor;
        this.storage = storage;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public VipPageConfigView getManagementView()
    {
        return buildView();
    }

    public VipPageConfigView getPublicView()
    {
        return buildView();
    }

    /** 修改客服提示语，并可同时替换客服微信图片。 */
    public int update(WlVipPageConfig request, MultipartFile image, String operator)
    {
        validate(request);
        String normalizedOperator = requireOperator(operator);
        WlVipPageConfig current = requireConfig();
        String oldKey = trimToNull(current.getCustomerServiceImageKey());
        request.setId(1L);
        request.setCustomerServiceTip(request.getCustomerServiceTip().trim());
        request.setUpdateBy(normalizedOperator);

        if (image == null || image.isEmpty())
        {
            request.setCustomerServiceImageKey(oldKey);
            return executeUpdate(request, oldKey);
        }

        String newKey = uploadNewImage(image);
        request.setCustomerServiceImageKey(newKey);
        try
        {
            int rows = executeUpdate(request, oldKey);
            cleanupOldObject(oldKey);
            return rows;
        }
        catch (RuntimeException exception)
        {
            cleanupNewObject(newKey);
            throw exception;
        }
    }

    private VipPageConfigView buildView()
    {
        WlVipPageConfig config = requireConfig();
        List<String> benefits = new ArrayList<>();
        List<WlVipBenefit> enabled = benefitService.listEnabled();
        if (enabled != null)
        {
            for (WlVipBenefit benefit : enabled)
            {
                if (benefit != null && benefit.getBenefitText() != null)
                    benefits.add(benefit.getBenefitText());
            }
        }
        return new VipPageConfigView(benefits, config.getCustomerServiceTip(),
                signedImageUrl(config.getCustomerServiceImageKey()));
    }

    private int executeUpdate(WlVipPageConfig request, String oldKey)
    {
        Integer rows;
        try
        {
            rows = transactionTemplate.execute(status ->
                    configMapper.updateConfigWithExpectedImage(request, oldKey));
        }
        catch (RuntimeException exception)
        {
            if (exception instanceof ServiceException) throw exception;
            throw new ServiceException("VIP 页面配置保存失败，请重试");
        }
        if (rows == null || rows != 1)
            throw new ServiceException("VIP 页面配置已发生变化，请刷新后重试");
        return rows;
    }

    private String uploadNewImage(MultipartFile image)
    {
        VipCustomerServiceImageProcessor.ProcessedImage processed = imageProcessor.process(image);
        String objectKey = "vip/customer-service/"
                + UUID.randomUUID().toString().replace("-", "")
                + "/wechat." + processed.getExtension();
        boolean uploadAttempted = false;
        try (VipCustomerServiceImageProcessor.ProcessedImage ignored = processed;
                InputStream input = processed.openStream())
        {
            uploadAttempted = true;
            storage.putPrivateObject(objectKey, input, processed.getSize(), processed.getContentType());
            return objectKey;
        }
        catch (IOException | RuntimeException exception)
        {
            if (uploadAttempted) cleanupNewObject(objectKey);
            throw new ServiceException("客服微信图片上传失败，请重试");
        }
    }

    private String signedImageUrl(String imageKey)
    {
        String key = trimToNull(imageKey);
        if (key == null) return null;
        try
        {
            URL signed = storage.signGetUrl(key, IMAGE_URL_TTL, null);
            if (signed == null) throw new ServiceException("客服微信图片服务暂不可用，请稍后重试");
            return signed.toString();
        }
        catch (RuntimeException exception)
        {
            throw new ServiceException("客服微信图片服务暂不可用，请稍后重试");
        }
    }

    private WlVipPageConfig requireConfig()
    {
        WlVipPageConfig config = configMapper.selectConfig();
        if (config == null) throw new ServiceException("VIP 页面配置不存在");
        return config;
    }

    private void validate(WlVipPageConfig request)
    {
        if (request == null || request.getCustomerServiceTip() == null
                || request.getCustomerServiceTip().trim().isEmpty())
            throw new ServiceException("客服提示语不能为空");
        if (request.getCustomerServiceTip().trim().length() > 100)
            throw new ServiceException("客服提示语不能超过100个字符");
    }

    private String requireOperator(String operator)
    {
        if (operator == null || operator.trim().isEmpty()) throw new ServiceException("操作人不能为空");
        return operator.trim();
    }

    private String trimToNull(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private void cleanupNewObject(String objectKey)
    {
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException exception)
        {
            LOGGER.error("新客服微信图片补偿删除失败，对象键：{}", objectKey);
        }
    }

    private void cleanupOldObject(String objectKey)
    {
        if (objectKey == null) return;
        try { storage.deleteObjectAfterMetadataDeletion(objectKey); }
        catch (RuntimeException exception)
        {
            LOGGER.warn("旧客服微信图片清理失败，对象键：{}", objectKey);
        }
    }
}
