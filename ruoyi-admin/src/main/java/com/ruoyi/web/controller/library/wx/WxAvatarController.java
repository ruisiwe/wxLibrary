package com.ruoyi.web.controller.library.wx;

import java.nio.file.Path;
import com.ruoyi.library.storage.AvatarStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序头像受控读取接口。 */
@RestController
@RequestMapping("/wx/public/avatar")
public class WxAvatarController
{
    private final AvatarStorageService avatarStorageService;

    public WxAvatarController(AvatarStorageService avatarStorageService)
    {
        this.avatarStorageService = avatarStorageService;
    }

    /** 按受控相对路径读取头像，不暴露服务器绝对目录。 */
    @GetMapping("/{month}/{filename:.+}")
    public ResponseEntity<Resource> read(@PathVariable String month, @PathVariable String filename)
    {
        Path file = avatarStorageService.resolveForRead(month + "/" + filename);
        return ResponseEntity.ok().contentType(mediaType(filename)).body(new FileSystemResource(file));
    }

    private MediaType mediaType(String filename)
    {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
