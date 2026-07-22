package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.dto.BannerDto;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 宣传图片数据访问。 */
public interface WlBannerMapper
{
    List<BannerDto> selectPublicBanners(@Param("currentTime") Date currentTime);
    WlBanner selectBannerById(@Param("id") Long id);
    List<WlBanner> selectBannerList(WlBanner query);
    int insertBanner(WlBanner banner);
    int updateBanner(WlBanner banner);
    int updateBannerWithExpectedImage(@Param("banner") WlBanner banner,
            @Param("expectedImageUrl") String expectedImageUrl);
    int deleteBannerWithExpectedImage(@Param("id") Long id,
            @Param("expectedImageUrl") String expectedImageUrl,
            @Param("operator") String operator);
    int deleteBanners(@Param("ids") Long[] ids, @Param("operator") String operator);
}
