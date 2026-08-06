package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.mapper.WlVipPageConfigMapper;
import com.ruoyi.library.storage.QrImageStorageService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** VIP 套餐页权益和客服微信配置服务。 */
@Service
public class VipPageConfigService
{
    private static final String CUSTOMER_IMAGE_URL =
            "/wx/public/vip-page-config/customer-service-image";

    private final WlVipPageConfigMapper configMapper;
    private final VipBenefitService benefitService;
    private final QrImageStorageService storage;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public VipPageConfigService(WlVipPageConfigMapper configMapper,
            VipBenefitService benefitService,
            QrImageStorageService storage,
            PlatformTransactionManager transactionManager)
    {
        this.configMapper = configMapper;
        this.benefitService = benefitService;
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

    /** 修改客服提示语，并可同时上传或替换客服微信图片。 */
    public int update(WlVipPageConfig request, MultipartFile image, String operator)
    {
        validate(request);
        String username = requireOperator(operator);
        WlVipPageConfig current = requireConfig();
        String oldPath = trimToNull(current.getCustomerServiceImageKey());
        request.setId(1L);
        request.setCustomerServiceTip(request.getCustomerServiceTip().trim());
        request.setUpdateBy(username);

        if (image == null || image.isEmpty())
        {
            request.setCustomerServiceImageKey(oldPath);
            return executeUpdate(request, oldPath);
        }

        String newPath = storage.storeVipCustomerService(image);
        request.setCustomerServiceImageKey(newPath);
        try
        {
            int rows = executeUpdate(request, oldPath);
            storage.deleteVipCustomerServiceQuietly(oldPath);
            return rows;
        }
        catch (RuntimeException exception)
        {
            storage.deleteVipCustomerServiceQuietly(newPath);
            throw exception;
        }
    }

    /** 清空客服微信图片，保留提示语配置。 */
    public int clearImage(String operator)
    {
        WlVipPageConfig current = requireConfig();
        String oldPath = trimToNull(current.getCustomerServiceImageKey());
        if (oldPath == null) return 1;

        WlVipPageConfig request = new WlVipPageConfig();
        request.setId(1L);
        request.setCustomerServiceTip(current.getCustomerServiceTip());
        request.setCustomerServiceImageKey(null);
        request.setUpdateBy(requireOperator(operator));
        int rows = executeUpdate(request, oldPath);
        storage.deleteVipCustomerServiceQuietly(oldPath);
        return rows;
    }

    /** 受控读取本地客服微信图片，不暴露服务器绝对路径。 */
    public Path resolveCustomerServiceImageForRead()
    {
        String path = trimToNull(requireConfig().getCustomerServiceImageKey());
        if (path == null) throw new ServiceException("客服微信图片暂未配置");
        return storage.resolveVipCustomerServiceForRead(path);
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
                localImageUrl(config.getCustomerServiceImageKey()));
    }

    private String localImageUrl(String imagePath)
    {
        String path = trimToNull(imagePath);
        if (path == null) return null;
        try
        {
            storage.resolveVipCustomerServiceForRead(path);
            return CUSTOMER_IMAGE_URL;
        }
        catch (RuntimeException exception)
        {
            // 旧 COS 对象键不会自动迁移，后台重新上传前按未配置处理。
            return null;
        }
    }

    private int executeUpdate(WlVipPageConfig request, String oldPath)
    {
        try
        {
            Integer rows = transactionTemplate.execute(status -> {
                int affected = configMapper.updateConfigWithExpectedImage(request, oldPath);
                if (affected != 1)
                    throw new ServiceException("VIP 页面配置已发生变化，请刷新后重试");
                return affected;
            });
            if (rows == null || rows != 1)
                throw new ServiceException("VIP 页面配置保存失败，请重试");
            return rows;
        }
        catch (RuntimeException exception)
        {
            if (exception instanceof ServiceException) throw exception;
            throw new ServiceException("VIP 页面配置保存失败，请重试");
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
        if (operator == null || operator.trim().isEmpty())
            throw new ServiceException("操作人不能为空");
        return operator.trim();
    }

    private String trimToNull(String value)
    {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
