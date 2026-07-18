package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.dto.CategoryDto;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.dto.HomeData;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.service.HomeQueryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 小程序匿名首页、分类和文档元数据接口。 */
@RestController
@RequestMapping("/wx/public")
public class WxPublicContentController
{
    private final HomeQueryService homeQueryService;

    public WxPublicContentController(HomeQueryService homeQueryService)
    {
        this.homeQueryService = homeQueryService;
    }

    /** 匿名查询首页有效宣传图片、分类和已上架文档。 */
    @GetMapping("/home")
    public WxApiResponse<HomeData> home(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        return WxApiResponse.success(homeQueryService.getHome(pageNum, pageSize));
    }

    /** 匿名查询已启用的文档分类。 */
    @GetMapping("/categories")
    public WxApiResponse<List<CategoryDto>> categories()
    {
        return WxApiResponse.success(homeQueryService.listCategories());
    }

    /** 匿名按关键词和分类分页查询已上架文档。 */
    @GetMapping("/documents")
    public WxApiResponse<PageResult<DocumentSummaryDto>> documents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize)
    {
        return WxApiResponse.success(
                homeQueryService.searchDocuments(keyword, categoryId, pageNum, pageSize));
    }

    /** 匿名查询已上架文档的公开基本信息。 */
    @GetMapping("/documents/{id}")
    public WxApiResponse<DocumentSummaryDto> document(@PathVariable Long id)
    {
        return WxApiResponse.success(homeQueryService.getDocument(id));
    }
}
