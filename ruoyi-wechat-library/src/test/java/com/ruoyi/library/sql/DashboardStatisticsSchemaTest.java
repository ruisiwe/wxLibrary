package com.ruoyi.library.sql;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardStatisticsSchemaTest
{
    @Test
    void createsDashboardStatisticsTablesWithRequiredIndexes() throws Exception
    {
        Path path = Paths.get("docs/2026-08-03-admin-dashboard-statistics.sql");
        if (!Files.exists(path))
        {
            path = Paths.get("../docs/2026-08-03-admin-dashboard-statistics.sql");
        }

        assertTrue(Files.exists(path), "缺少后台首页统计建表 SQL");
        String sql = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("create table if not exists `wl_wx_user_daily_active`"));
        assertTrue(sql.contains("unique key `uk_wx_user_daily_active` (`user_id`, `active_date`)"));
        assertTrue(sql.contains("key `idx_wx_user_daily_active_date` (`active_date`)"));
        assertTrue(sql.contains("create table if not exists `wl_document_send_record`"));
        assertTrue(sql.contains("unique key `uk_document_send_request` (`request_id`)"));
        assertTrue(sql.contains("key `idx_document_send_document_time` (`document_id`, `send_time`)"));
        assertTrue(sql.contains("key `idx_document_send_time` (`send_time`)"));
        assertTrue(sql.contains("key `idx_document_unlock_dashboard`"));
        assertFalse(sql.contains("foreign key"));
    }
}
