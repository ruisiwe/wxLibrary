package com.ruoyi.library.mapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMapperXmlTest
{
    @Test
    void dailyActiveWriteIsIdempotent() throws Exception
    {
        Configuration configuration = parse("mapper/library/WlWxUserMapper.xml");
        String sql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlWxUserMapper.insertDailyActive")
                .getBoundSql(Collections.singletonMap("id", 9L)).getSql().toLowerCase();

        assertTrue(sql.contains("insert ignore into wl_wx_user_daily_active"));
        assertTrue(sql.contains("curdate()"));
    }

    @Test
    void documentSendWriteIsIdempotent() throws Exception
    {
        Configuration configuration = parse("mapper/library/WlDocumentSendRecordMapper.xml");
        String insertSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlDocumentSendRecordMapper.insertRecord")
                .getBoundSql(new Object()).getSql().toLowerCase();
        String selectSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlDocumentSendRecordMapper.selectByRequestId")
                .getBoundSql(Collections.singletonMap("requestId", "send-8-1"))
                .getSql().toLowerCase();
        String currentReadSql = configuration
                .getMappedStatement("com.ruoyi.library.mapper.WlDocumentSendRecordMapper.selectByRequestIdForUpdate")
                .getBoundSql(Collections.singletonMap("requestId", "send-8-1"))
                .getSql().toLowerCase();

        assertTrue(insertSql.contains("insert ignore into wl_document_send_record"));
        assertTrue(selectSql.contains("del_flag = '0'"));
        assertTrue(currentReadSql.contains("for update"));
    }

    @Test
    void dashboardQueriesUseConfirmedBusinessFilters() throws Exception
    {
        Configuration configuration = parse("mapper/library/LibraryDashboardMapper.xml");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("now", new Date());
        parameters.put("start", new Date());
        parameters.put("end", new Date());
        parameters.put("startDate", "2026-07-28");
        parameters.put("endDate", "2026-08-04");

        String summarySql = sql(configuration, "selectSummary", parameters);
        String monthlySql = sql(configuration, "selectMonthlyPaidExchangeCounts", parameters);
        String categorySql = sql(configuration, "selectCategoryDocumentCounts", parameters);
        String sendSql = sql(configuration, "selectCategorySendCounts", parameters);

        assertTrue(summarySql.contains("from wl_wx_user"));
        assertTrue(summarySql.contains("vip_expire_time >"));
        assertFalse(summarySql.contains("status = '0'"));
        assertTrue(summarySql.contains("from wl_document_unlock"));
        assertTrue(summarySql.contains("spent_points > 0"));
        assertTrue(monthlySql.contains("date_format(u.unlock_time, '%y-%m')"));
        assertTrue(monthlySql.contains("u.spent_points > 0"));
        assertTrue(categorySql.contains("from wl_category c"));
        assertTrue(categorySql.contains("left join wl_document d"));
        assertTrue(categorySql.contains("d.del_flag = '0'"));
        assertTrue(sendSql.contains("from wl_document_send_record s"));
        assertTrue(sendSql.contains("s.del_flag = '0'"));
    }

    private String sql(Configuration configuration, String statement, Object parameter)
    {
        return configuration
                .getMappedStatement("com.ruoyi.library.mapper.LibraryDashboardMapper." + statement)
                .getBoundSql(parameter).getSql().toLowerCase();
    }

    private Configuration parse(String resource) throws Exception
    {
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input, resource);
            new XMLMapperBuilder(input, configuration, resource,
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }
}
