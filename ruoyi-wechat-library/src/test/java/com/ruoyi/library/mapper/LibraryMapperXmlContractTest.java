package com.ruoyi.library.mapper;

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
                "mapper/library/WlDocumentMapper.xml");
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
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlCategoryMapper.selectPublicCategories"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlDocumentMapper.selectPublishedDocuments"));

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
    }
}
