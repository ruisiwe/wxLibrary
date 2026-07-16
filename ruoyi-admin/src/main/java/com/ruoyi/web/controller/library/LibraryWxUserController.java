package com.ruoyi.web.controller.library;

import java.util.ArrayList;
import java.util.List;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.LibraryWxUserView;
import com.ruoyi.library.dto.PointAdjustmentRequest;
import com.ruoyi.library.service.LibraryWxUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台微信用户管理接口。 */
@RestController
@RequestMapping("/library/wx-user")
public class LibraryWxUserController extends BaseController
{
    private final LibraryWxUserService userService;

    public LibraryWxUserController(LibraryWxUserService userService) { this.userService = userService; }

    /** 分页查询微信用户，openid 仅返回脱敏值。 */
    @PreAuthorize("@ss.hasPermi('library:wxUser:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlWxUser query)
    {
        startPage();
        List<WlWxUser> users = userService.list(query);
        long total = new PageInfo<>(users).getTotal();
        List<LibraryWxUserView> views = new ArrayList<>();
        for (WlWxUser user : users) views.add(LibraryWxUserView.from(user));
        return new TableDataInfo(views, total);
    }

    /** 查询微信用户详情。 */
    @PreAuthorize("@ss.hasPermi('library:wxUser:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(userService.detail(id)); }

    /** 启用或停用微信用户。 */
    @PreAuthorize("@ss.hasPermi('library:wxUser:edit')")
    @PutMapping("/{id}/status")
    public AjaxResult status(@PathVariable Long id, @RequestParam String status)
    {
        return toAjax(userService.changeStatus(id, status, getUsername()));
    }

    /** 人工调整积分，业务编号用于幂等控制。 */
    @PreAuthorize("@ss.hasPermi('library:wxUser:points')")
    @PostMapping("/{id}/points")
    public AjaxResult points(@PathVariable Long id, @RequestBody PointAdjustmentRequest request)
    {
        return success(userService.adjustPoints(id, request, getUsername()));
    }
}
