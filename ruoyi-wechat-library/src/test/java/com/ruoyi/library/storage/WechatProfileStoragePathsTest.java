package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WechatProfileStoragePathsTest
{
    @TempDir
    Path root;

    @Test
    void resolvesSeparatedAvatarAndDocumentTempDirectories()
    {
        WechatProfileStoragePaths paths = new WechatProfileStoragePaths(() -> root.toString());

        assertEquals(root.resolve("avatar").toAbsolutePath().normalize(), paths.avatarRoot());
        assertEquals(root.resolve("document-temp").toAbsolutePath().normalize(), paths.documentTempRoot());
    }

    @Test
    void rejectsBlankRoot()
    {
        ServiceException error = assertThrows(ServiceException.class,
                () -> new WechatProfileStoragePaths(() -> " ").avatarRoot());

        assertEquals("微信资料存储根目录尚未配置", error.getMessage());
    }

    @Test
    void rejectsInvalidRootWithSafeChineseMessage()
    {
        ServiceException error = assertThrows(ServiceException.class,
                () -> new WechatProfileStoragePaths(() -> "invalid\0path").documentTempRoot());

        assertEquals("微信资料存储路径不合法", error.getMessage());
    }
}
