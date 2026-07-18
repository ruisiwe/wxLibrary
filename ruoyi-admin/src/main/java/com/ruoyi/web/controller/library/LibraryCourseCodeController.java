package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlCourseCode;
import com.ruoyi.library.dto.CourseCodeGenerateRequest;
import com.ruoyi.library.service.CourseCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台课程码批量生成和查询接口。 */
@RestController
@RequestMapping("/library/course-code")
public class LibraryCourseCodeController extends BaseController
{
    private final CourseCodeService service;

    public LibraryCourseCodeController(CourseCodeService service) { this.service = service; }

    /** 分页查询课程码掩码和使用状态。 */
    @PreAuthorize("@ss.hasPermi('library:course:code')")
    @GetMapping("/list")
    public TableDataInfo list(WlCourseCode query)
    {
        startPage();
        return getDataTable(service.list(query));
    }

    /** 批量生成高熵课程码，明文只在本次响应中返回且禁止写入操作日志。 */
    @PreAuthorize("@ss.hasPermi('library:course:code')")
    @Log(title = "课程码生成", businessType = BusinessType.INSERT,
            isSaveResponseData = false)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody CourseCodeGenerateRequest request)
    {
        if (request == null || request.getCount() == null)
            throw new ServiceException("生成数量不能为空");
        return success(service.generate(request.getCourseId(), request.getCount(),
                request.getExpiresTime(), getUsername()));
    }
}
