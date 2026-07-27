package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlVipPageConfig;
import org.apache.ibatis.annotations.Param;

/** VIP 套餐页面配置数据访问。 */
public interface WlVipPageConfigMapper
{
    WlVipPageConfig selectConfig();

    int updateConfigWithExpectedImage(@Param("config") WlVipPageConfig config,
            @Param("expectedImageKey") String expectedImageKey);
}
