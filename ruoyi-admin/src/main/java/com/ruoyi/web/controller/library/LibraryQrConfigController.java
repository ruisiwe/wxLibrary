package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlQrConfig;
import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.service.QrConfigService;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台通用二维码管理接口。 */
@RestController
@RequestMapping("/library/qr-config")
public class LibraryQrConfigController extends BaseController
{
    private final QrConfigService service;

    public LibraryQrConfigController(QrConfigService service)
    {
        this.service = service;
    }

    /** 分页查询二维码配置。 */
    @PreAuthorize("@ss.hasPermi('library:qr:query')")
    @GetMapping("/list")
    public TableDataInfo list(WlQrConfig query)
    {
        startPage();
        List<QrConfigView> list = service.list(query);
        return getDataTable(list);
    }

    /** 查询二维码配置详情。 */
    @PreAuthorize("@ss.hasPermi('library:qr:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(service.get(id));
    }

    /** 读取二维码配置图片。 */
    @PreAuthorize("@ss.hasPermi('library:qr:query')")
    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Long id)
    {
        Path file = service.resolveImageForManagement(id);
        return ResponseEntity.ok().contentType(mediaType(file)).body(new FileSystemResource(file));
    }

    /** 新增二维码配置。 */
    @PreAuthorize("@ss.hasPermi('library:qr:add')")
    @Log(title = "二维码管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlQrConfig config)
    {
        return toAjax(service.add(config, getUsername()));
    }

    /** 修改二维码配置文字、排序和状态。 */
    @PreAuthorize("@ss.hasPermi('library:qr:edit')")
    @Log(title = "二维码管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlQrConfig config)
    {
        return toAjax(service.edit(config, getUsername()));
    }

    /** 上传或替换二维码图片。 */
    @PreAuthorize("@ss.hasPermi('library:qr:edit')")
    @Log(title = "二维码图片", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult uploadImage(@PathVariable Long id, @RequestPart("image") MultipartFile image)
    {
        return toAjax(service.uploadImage(id, image, getUsername()));
    }

    /** 清空二维码图片，保留配置和菜单入口。 */
    @PreAuthorize("@ss.hasPermi('library:qr:edit')")
    @Log(title = "二维码图片", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{id}/image")
    public AjaxResult clearImage(@PathVariable Long id)
    {
        return toAjax(service.clearImage(id, getUsername()));
    }

    /** 删除二维码配置。 */
    @PreAuthorize("@ss.hasPermi('library:qr:remove')")
    @Log(title = "二维码管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(service.remove(id, getUsername()));
    }

    private MediaType mediaType(Path path)
    {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (filename.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
