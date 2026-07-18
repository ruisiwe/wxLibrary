package com.ruoyi.web.controller.library;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.service.PointService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台积分流水查询接口。 */
@RestController
@RequestMapping("/library/point-record")
public class LibraryPointRecordController extends BaseController
{
    private final PointService pointService;

    public LibraryPointRecordController(PointService pointService) { this.pointService = pointService; }

    /** 分页查询积分流水，可按用户、事件类型和业务编号筛选。 */
    @PreAuthorize("@ss.hasPermi('library:points:record')")
    @GetMapping("/list")
    public TableDataInfo list(WlPointRecord query)
    {
        startPage();
        List<WlPointRecord> list = pointService.listAdminRecords(query);
        return getDataTable(list);
    }
}
