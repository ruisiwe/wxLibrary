package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlBanner;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryMapperXmlContractTest
{
    @Test
    void mapperXmlFilesParseAndExposeBoundStatements() throws Exception
    {
        List<String> resources = Arrays.asList(
                "mapper/library/WlWxUserMapper.xml",
                "mapper/library/WlAgreementMapper.xml",
                "mapper/library/WlUserAgreementMapper.xml",
                "mapper/library/WlPointRecordMapper.xml",
                "mapper/library/WlBannerMapper.xml",
                "mapper/library/WlCategoryMapper.xml",
                "mapper/library/WlDocumentMapper.xml",
                "mapper/library/WlDocumentConversionMapper.xml",
                "mapper/library/WlPointMapper.xml",
                "mapper/library/WlDocumentUnlockMapper.xml",
                "mapper/library/WlVipPlanMapper.xml",
                "mapper/library/WlVipEntitlementMapper.xml",
                "mapper/library/WlVipOrderMapper.xml",
                "mapper/library/WlVipRefundMapper.xml",
                "mapper/library/WlCourseMapper.xml",
                "mapper/library/WlCourseCodeMapper.xml",
                "mapper/library/WlUserCourseMapper.xml",
                "mapper/library/WlVideoProgressMapper.xml");
        Configuration configuration = new Configuration();
        for (String resource : resources)
        {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource))
            {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlWxUserMapper.selectByOpenidForUpdate"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlWxUserMapper.selectByOpenid"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlAgreementMapper.disablePublishedByType"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlUserAgreementMapper.countAcceptedAgreementIds"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlPointRecordMapper.insertPointRecord"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlBannerMapper.selectPublicBanners"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlBannerMapper.updateBannerWithExpectedImage"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlBannerMapper.deleteBannerWithExpectedImage"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlCategoryMapper.selectPublicCategories"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentMapper.selectPublishedDocuments"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentMapper.countBannerDocumentOptions"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentMapper.selectBannerDocumentOptions"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentConversionMapper.markConverting"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlPointMapper.deductIfEnough"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentUnlockMapper.selectUnlock"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlVipPlanMapper.selectEnabledById"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlVipEntitlementMapper.insertEntitlement"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlVipOrderMapper.markPaid"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlVipRefundMapper.markSuccess"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlCourseCodeMapper.selectByDigestForUpdate"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlVideoProgressMapper.upsertProgress"));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("keyword", "质量");
        parameters.put("categoryId", 3L);
        parameters.put("offset", 0L);
        parameters.put("limit", 10);
        String publicSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlDocumentMapper.selectPublishedDocuments")
                .getBoundSql(parameters).getSql().toLowerCase();
        assertFalse(publicSql.contains("object_key"));
        assertTrue(publicSql.contains("d.tags like"));
        assertTrue(publicSql.contains("order by d.sort_order asc, d.id desc"));

        Map<String, Object> optionParameters = new HashMap<>();
        optionParameters.put("keyword", "质量");
        optionParameters.put("offset", 0L);
        optionParameters.put("limit", 20);
        String optionSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlDocumentMapper.selectBannerDocumentOptions")
                .getBoundSql(optionParameters).getSql().toLowerCase();
        assertFalse(optionSql.contains("object_key"));
        assertTrue(optionSql.contains("d.publish_status = 'published'"));
        assertTrue(optionSql.contains("c.status = '0'"));
        assertTrue(optionSql.contains("d.title like"));
        assertTrue(optionSql.contains("order by d.publish_time desc, d.id desc"));

        String managementBannerSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlBannerMapper.selectBannerList")
                .getBoundSql(new WlBanner()).getSql().toLowerCase();
        assertTrue(managementBannerSql.contains("document_title"));
        assertTrue(managementBannerSql.contains("document_category_name"));
        assertTrue(managementBannerSql.contains("document_file_format"));
        assertTrue(managementBannerSql.contains("document_selectable"));

        WlBanner banner = new WlBanner();
        banner.setId(4L);
        banner.setDocumentId(8L);
        banner.setTitle("首页推荐");
        banner.setImageUrl("banners/new/image.jpg");
        Map<String, Object> mutationParameters = new HashMap<>();
        mutationParameters.put("banner", banner);
        mutationParameters.put("expectedImageUrl", "banners/old/image.jpg");
        mutationParameters.put("id", 4L);
        mutationParameters.put("operator", "admin");
        String updateSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlBannerMapper.updateBannerWithExpectedImage")
                .getBoundSql(mutationParameters).getSql().toLowerCase();
        assertTrue(updateSql.contains("image_url = ?"));
        assertTrue(updateSql.contains("and image_url = ?"));
        assertTrue(updateSql.contains("d.publish_status = 'published'"));
        String deleteSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlBannerMapper.deleteBannerWithExpectedImage")
                .getBoundSql(mutationParameters).getSql().toLowerCase();
        assertTrue(deleteSql.contains("where id = ?"));
        assertTrue(deleteSql.contains("and image_url = ?"));
    }
}
