package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlCategory;
import com.ruoyi.library.dto.CategoryDto;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文档分类数据访问。 */
public interface WlCategoryMapper
{
    List<CategoryDto> selectPublicCategories();
    WlCategory selectCategoryById(@Param("id") Long id);
    List<WlCategory> selectCategoryList(WlCategory query);
    int countCategoryName(@Param("name") String name, @Param("excludeId") Long excludeId);
    int insertCategory(WlCategory category);
    int updateCategory(WlCategory category);
    int deleteCategories(@Param("ids") Long[] ids, @Param("operator") String operator);
}
