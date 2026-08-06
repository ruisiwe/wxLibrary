package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.service.VipPageConfigService;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序公开读取 VIP 客服微信二维码的受控接口。 */
@RestController
@RequestMapping("/wx/public/vip-page-config")
public class WxVipPageImageController
{
    private final VipPageConfigService service;

    public WxVipPageImageController(VipPageConfigService service)
    {
        this.service = service;
    }

    /** 读取后台当前配置的本地客服微信二维码。 */
    @GetMapping("/customer-service-image")
    public ResponseEntity<Resource> customerServiceImage()
    {
        Path file = service.resolveCustomerServiceImageForRead();
        return ResponseEntity.ok().contentType(mediaType(file)).body(new FileSystemResource(file));
    }

    private MediaType mediaType(Path path)
    {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
