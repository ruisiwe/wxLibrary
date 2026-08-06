package com.ruoyi.web.controller.library;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.library.service.LibraryDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台首页文库统计接口。 */
@RestController
@RequestMapping("/library/dashboard")
public class LibraryDashboardController extends BaseController
{
    private final LibraryDashboardService service;

    public LibraryDashboardController(LibraryDashboardService service)
    {
        this.service = service;
    }

    /** 查询后台首页全部文库统计数据。 */
    @GetMapping
    public AjaxResult dashboard()
    {
        return success(service.load());
    }
}
