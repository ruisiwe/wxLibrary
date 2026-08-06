package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.service.QrConfigService;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序登录用户的通用二维码接口。 */
@RestController
@RequestMapping("/wx/qr-configs")
public class WxQrConfigController
{
    private final QrConfigService service;

    public WxQrConfigController(QrConfigService service)
    {
        this.service = service;
    }

    /** 按后台排序查询全部启用的二维码菜单。 */
    @GetMapping
    public WxApiResponse<List<QrConfigView>> list()
    {
        return WxApiResponse.success(service.listEnabled());
    }

    /** 查询一条启用的二维码展示信息。 */
    @GetMapping("/{id}")
    public WxApiResponse<QrConfigView> detail(@PathVariable Long id)
    {
        return WxApiResponse.success(service.getEnabled(id));
    }

    /** 受控读取启用二维码的本地图片。 */
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id)
    {
        Path file = service.resolveEnabledImage(id);
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
