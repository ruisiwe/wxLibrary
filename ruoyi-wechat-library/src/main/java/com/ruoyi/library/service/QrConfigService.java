package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlQrConfig;
import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.mapper.WlQrConfigMapper;
import com.ruoyi.library.storage.QrImageStorageService;
import com.github.pagehelper.Page;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 通用二维码配置维护服务。 */
@Service
public class QrConfigService
{
    private final WlQrConfigMapper mapper;
    private final QrImageStorageService storage;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public QrConfigService(WlQrConfigMapper mapper, QrImageStorageService storage,
            PlatformTransactionManager transactionManager)
    {
        this.mapper = mapper;
        this.storage = storage;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<QrConfigView> list(WlQrConfig query)
    {
        return views(mapper.selectList(query == null ? new WlQrConfig() : query));
    }

    public List<QrConfigView> listEnabled()
    {
        return views(mapper.selectEnabled());
    }

    public QrConfigView get(Long id)
    {
        return view(requireConfig(id));
    }

    public QrConfigView getEnabled(Long id)
    {
        WlQrConfig config = requireConfig(id);
        if (!"0".equals(config.getStatus()))
            throw new ServiceException("二维码配置不存在或已停用");
        return view(config);
    }

    public int add(WlQrConfig config, String operator)
    {
        validate(config, false);
        config.setCreateBy(requireOperator(operator));
        int rows = mapper.insertConfig(config);
        if (rows != 1) throw new ServiceException("二维码配置保存失败，请重试");
        return rows;
    }

    public int edit(WlQrConfig config, String operator)
    {
        validate(config, true);
        requireConfig(config.getId());
        config.setUpdateBy(requireOperator(operator));
        int rows = mapper.updateConfig(config);
        if (rows != 1) throw new ServiceException("二维码配置已发生变化，请刷新后重试");
        return rows;
    }

    public int uploadImage(Long id, MultipartFile image, String operator)
    {
        WlQrConfig current = requireConfig(id);
        String username = requireOperator(operator);
        String newPath = storage.storeQrConfig(image);
        try
        {
            Integer rows = transactionTemplate.execute(status -> {
                int affected = mapper.updateImageWithExpectedPath(
                        id, newPath, current.getImagePath(), username);
                if (affected != 1)
                    throw new ServiceException("二维码配置已发生变化，请刷新后重试");
                return affected;
            });
            if (rows == null || rows != 1)
                throw new ServiceException("二维码图片保存失败，请重试");
        }
        catch (RuntimeException exception)
        {
            storage.deleteQrConfigQuietly(newPath);
            throw safeException(exception, "二维码图片保存失败，请重试");
        }
        storage.deleteQrConfigQuietly(current.getImagePath());
        return 1;
    }

    public int clearImage(Long id, String operator)
    {
        WlQrConfig current = requireConfig(id);
        String username = requireOperator(operator);
        if (isBlank(current.getImagePath())) return 1;
        Integer rows = transactionTemplate.execute(status -> {
            int affected = mapper.updateImageWithExpectedPath(
                    id, null, current.getImagePath(), username);
            if (affected != 1)
                throw new ServiceException("二维码配置已发生变化，请刷新后重试");
            return affected;
        });
        if (rows == null || rows != 1) throw new ServiceException("二维码图片清空失败，请重试");
        storage.deleteQrConfigQuietly(current.getImagePath());
        return rows;
    }

    public int remove(Long id, String operator)
    {
        WlQrConfig current = requireConfig(id);
        String username = requireOperator(operator);
        Integer rows = transactionTemplate.execute(status -> {
            int affected = mapper.deleteConfigWithExpectedPath(id, current.getImagePath(), username);
            if (affected != 1)
                throw new ServiceException("二维码配置已发生变化，请刷新后重试");
            return affected;
        });
        if (rows == null || rows != 1) throw new ServiceException("二维码配置删除失败，请重试");
        storage.deleteQrConfigQuietly(current.getImagePath());
        return rows;
    }

    public Path resolveImageForManagement(Long id)
    {
        return storage.resolveQrConfigForRead(requireImagePath(requireConfig(id)));
    }

    public Path resolveEnabledImage(Long id)
    {
        WlQrConfig config = requireConfig(id);
        if (!"0".equals(config.getStatus()))
            throw new ServiceException("二维码配置不存在或已停用");
        return storage.resolveQrConfigForRead(requireImagePath(config));
    }

    private List<QrConfigView> views(List<WlQrConfig> configs)
    {
        List<QrConfigView> result;
        if (configs instanceof Page)
        {
            Page<?> source = (Page<?>) configs;
            Page<QrConfigView> page = new Page<>(source.getPageNum(), source.getPageSize());
            page.setTotal(source.getTotal());
            result = page;
        }
        else
        {
            result = new ArrayList<>();
        }
        if (configs == null) return result;
        for (WlQrConfig config : configs) result.add(view(config));
        return result;
    }

    private QrConfigView view(WlQrConfig config)
    {
        boolean configured = !isBlank(config.getImagePath());
        String imageUrl = configured ? "/wx/qr-configs/" + config.getId() + "/image" : null;
        return new QrConfigView(config.getId(), config.getMenuName(), config.getGuideText(),
                config.getSortOrder(), config.getStatus(), configured, imageUrl);
    }

    private WlQrConfig requireConfig(Long id)
    {
        requireId(id);
        WlQrConfig config = mapper.selectById(id);
        if (config == null) throw new ServiceException("二维码配置不存在");
        return config;
    }

    private String requireImagePath(WlQrConfig config)
    {
        if (isBlank(config.getImagePath())) throw new ServiceException("二维码图片暂未配置");
        return config.getImagePath().trim();
    }

    private void validate(WlQrConfig config, boolean requireId)
    {
        if (config == null) throw new ServiceException("二维码配置参数不能为空");
        if (requireId) requireId(config.getId());
        if (isBlank(config.getMenuName())) throw new ServiceException("菜单名称不能为空");
        String menuName = config.getMenuName().trim();
        if (menuName.length() > 50) throw new ServiceException("菜单名称不能超过50个字符");
        String guideText = config.getGuideText() == null ? "" : config.getGuideText().trim();
        if (guideText.length() > 200) throw new ServiceException("引导文字不能超过200个字符");
        if (config.getSortOrder() == null) config.setSortOrder(0);
        if (config.getSortOrder() < 0) throw new ServiceException("排序不能小于0");
        if (isBlank(config.getStatus())) config.setStatus("0");
        if (!"0".equals(config.getStatus()) && !"1".equals(config.getStatus()))
            throw new ServiceException("二维码状态不正确");
        config.setMenuName(menuName);
        config.setGuideText(guideText);
    }

    private void requireId(Long id)
    {
        if (id == null || id <= 0) throw new ServiceException("二维码配置编号不能为空");
    }

    private String requireOperator(String operator)
    {
        if (isBlank(operator)) throw new ServiceException("操作人不能为空");
        return operator.trim();
    }

    private RuntimeException safeException(RuntimeException exception, String fallback)
    {
        return exception instanceof ServiceException ? exception : new ServiceException(fallback);
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
