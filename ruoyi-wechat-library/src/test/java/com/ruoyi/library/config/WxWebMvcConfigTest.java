package com.ruoyi.library.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WxWebMvcConfigTest
{
    @Test
    void configProtectsWechatApisAndExcludesPublicEntrypoints()
            throws Exception
    {
        Path source = locate("src/main/java/com/ruoyi/library/config/WxWebMvcConfig.java");
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("addPathPatterns(\"/wx/**\")"));
        assertTrue(text.contains("\"/wx/auth/login\""));
        assertTrue(text.contains("\"/wx/public/**\""));
        assertTrue(text.contains("\"/wx/pay/notify\""));
    }

    private Path locate(String relative)
    {
        Path modulePath = Paths.get(relative);
        if (Files.exists(modulePath))
        {
            return modulePath;
        }
        return Paths.get("ruoyi-wechat-library").resolve(relative);
    }
}
