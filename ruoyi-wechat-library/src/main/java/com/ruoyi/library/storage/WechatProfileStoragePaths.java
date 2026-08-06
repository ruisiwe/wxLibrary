package com.ruoyi.library.storage;

import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 微信资料本地存储路径解析器。 */
@Component
public class WechatProfileStoragePaths
{
    private final Supplier<String> rootSupplier;

    @Autowired
    public WechatProfileStoragePaths(RuoYiConfig config)
    {
        Objects.requireNonNull(config, "RuoYiConfig 不能为空");
        this.rootSupplier = RuoYiConfig::getWechatProfile;
    }

    WechatProfileStoragePaths(Supplier<String> rootSupplier)
    {
        this.rootSupplier = rootSupplier;
    }

    public Path avatarRoot()
    {
        return child("avatar");
    }

    public Path documentTempRoot()
    {
        return child("document-temp");
    }

    public Path qrConfigRoot()
    {
        return child("qr-config");
    }

    public Path vipCustomerServiceRoot()
    {
        return child("vip-customer-service");
    }

    private Path child(String name)
    {
        String configured = rootSupplier.get();
        if (configured == null || configured.trim().isEmpty())
        {
            throw new ServiceException("微信资料存储根目录尚未配置");
        }
        Path root;
        try
        {
            root = Paths.get(configured.trim()).toAbsolutePath().normalize();
        }
        catch (InvalidPathException exception)
        {
            throw new ServiceException("微信资料存储路径不合法");
        }
        Path child = root.resolve(name).normalize();
        if (!child.startsWith(root) || !root.equals(child.getParent()))
        {
            throw new ServiceException("微信资料存储路径不合法");
        }
        return child;
    }
}
