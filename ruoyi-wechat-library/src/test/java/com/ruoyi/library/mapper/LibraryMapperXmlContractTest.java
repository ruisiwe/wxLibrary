package com.ruoyi.library.mapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

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
                "mapper/library/WlPointRecordMapper.xml");
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
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlAgreementMapper.disablePublishedByType"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlUserAgreementMapper.countAcceptedAgreementIds"));
        assertTrue(configuration.hasStatement("com.ruoyi.library.mapper.WlPointRecordMapper.insertPointRecord"));
    }
}
