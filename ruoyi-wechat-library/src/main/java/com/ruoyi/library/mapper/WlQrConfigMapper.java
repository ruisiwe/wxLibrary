package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlQrConfig;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 通用二维码配置数据访问。 */
public interface WlQrConfigMapper
{
    WlQrConfig selectById(@Param("id") Long id);

    List<WlQrConfig> selectList(WlQrConfig query);

    List<WlQrConfig> selectEnabled();

    int insertConfig(WlQrConfig config);

    int updateConfig(WlQrConfig config);

    int updateImageWithExpectedPath(@Param("id") Long id,
                                    @Param("newImagePath") String newImagePath,
                                    @Param("expectedImagePath") String expectedImagePath,
                                    @Param("operator") String operator);

    int deleteConfigWithExpectedPath(@Param("id") Long id,
                                     @Param("expectedImagePath") String expectedImagePath,
                                     @Param("operator") String operator);
}
