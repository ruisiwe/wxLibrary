package com.ruoyi.library.sql;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WechatLibrarySchemaTest
{
    private static String sql;

    @BeforeAll
    static void loadSql() throws Exception
    {
        Path path = Paths.get("sql/wechat_library.sql");
        if (!Files.exists(path))
        {
            path = Paths.get("../sql/wechat_library.sql");
        }
        sql = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).toLowerCase();
    }

    @Test
    void createsExactlyAllTwentyEightCoreTables()
    {
        List<String> tables = Arrays.asList("wl_wx_user", "wl_agreement", "wl_user_agreement", "wl_banner",
                "wl_category", "wl_document", "wl_document_conversion", "wl_document_unlock", "wl_favorite",
                "wl_document_view", "wl_point_rule", "wl_point_record", "wl_signin_record", "wl_ad_reward_record",
                "wl_share_task_record", "wl_invitation", "wl_course", "wl_course_video", "wl_course_code",
                "wl_user_course", "wl_video_progress", "wl_vip_plan", "wl_vip_code", "wl_vip_order", "wl_vip_entitlement",
                "wl_vip_refund", "wl_vip_benefit", "wl_vip_page_config");
        Matcher matcher = Pattern.compile("create\\s+table\\s+`?(wl_[a-z_]+)`?").matcher(sql);
        int count = 0;
        while (matcher.find())
        {
            count++;
            assertTrue(tables.contains(matcher.group(1)), "出现非核心表：" + matcher.group(1));
        }
        assertEquals(28, count);
        for (String table : tables)
        {
            assertTrue(sql.contains("create table `" + table + "`"), "缺少表：" + table);
        }
    }

    @Test
    void containsVipBenefitIntroductionTablesAndDefaults()
    {
        assertTrue(sql.contains("`benefit_text` varchar(100) not null"));
        assertTrue(sql.contains("`customer_service_image_key` varchar(512) default null"));
        assertTrue(sql.contains("`customer_service_tip` varchar(100) not null"));
        assertTrue(sql.contains("insert into `wl_vip_page_config`"));
        assertTrue(sql.contains("'开通 vip 请添加客服微信'"));
        assertTrue(sql.contains("'赠送积分'"));
        assertTrue(sql.contains("'vip 文档免费下载'"));
        assertTrue(sql.contains("'vip 专属课件'"));
    }

    @Test
    void keepsWechatUsersIndependentAndBalancesNonNegative()
    {
        assertFalse(sql.contains("foreign key"));
        assertFalse(sql.contains("sys_user"));
        assertTrue(sql.contains("unique key `uk_wx_user_openid` (`openid`)"));
        assertTrue(sql.contains("`point_balance` bigint not null default 0"));
        assertTrue(sql.contains("check (`point_balance` >= 0)"));
    }

    @Test
    void containsCriticalBusinessUniquenessAndSecurityConstraints()
    {
        assertTrue(sql.contains("unique key `uk_document_unlock_user_document` (`user_id`, `document_id`)"));
        assertTrue(sql.contains("unique key `uk_course_code_digest` (`code_digest`)"));
        assertTrue(sql.contains("unique key `uk_vip_code_digest` (`code_digest`)"));
        assertFalse(sql.contains("`code_plain`"));
        assertTrue(sql.contains("check (`access_type` in ('vip', 'code'))"));
        assertTrue(sql.contains("check (`access_type` in ('point','vip_free'))"));
        assertTrue(sql.contains("unique key `uk_point_record_biz_no` (`biz_no`)"));
        assertTrue(sql.contains("unique key `uk_vip_order_merchant_order_no` (`merchant_order_no`)"));
        assertTrue(sql.contains("unique key `uk_vip_refund_order_id` (`order_id`)"));
        assertTrue(sql.contains("`unrecovered_points` bigint not null default 0"));
        assertTrue(sql.contains("`vip_expire_time` datetime default null"));
        assertTrue(sql.contains("check (`source_type` in ('payment','manual','compensation','vip_code'))"));
        assertTrue(sql.contains("`operator_id` bigint default null"));
        assertTrue(sql.contains("`old_expire_time` datetime default null"));
        assertTrue(sql.contains("`new_expire_time` datetime not null"));
        assertTrue(sql.contains("`currency` char(3) not null default 'cny'"));
        assertTrue(sql.contains("'refund_processing','refunded'"));
        assertTrue(sql.contains("`operator_id` bigint not null comment '发起退款的后台操作人'"));
        assertTrue(sql.contains("'processing','accepted','success','failed'"));
        assertTrue(sql.contains("check (`status` in ('unused','used','disabled'))"));
        assertTrue(sql.contains("check (`is_permanent` = '1')"));
    }

    @Test
    void documentMetadataSupportsApprovedPublicContract()
    {
        assertTrue(sql.contains("`tags` varchar(500) not null default ''"));
        assertTrue(sql.contains("`uploader_name` varchar(128) not null"));
        assertTrue(sql.contains("`page_count` int not null default 0"));
        assertTrue(sql.contains("`sort_order` int not null default 0"));
        assertTrue(sql.contains("constraint `chk_document_page_count` check (`page_count` >= 0)"));
        assertTrue(sql.contains("constraint `chk_document_preview_boundary` check (`preview_pages` <= `page_count`)"));
        assertTrue(sql.contains("constraint `chk_document_file_size` check (`file_size` >= 0)"));
        assertTrue(sql.contains("`full_object_key` varchar(512) default null"));
        assertTrue(sql.contains("constraint `chk_document_conversion_status` check (`task_status` in ('pending','converting','success','failed'))"));
        assertTrue(sql.contains("('ad_reward', '激励视频广告', 1, 5, '0'"));
    }

    @Test
    void keepsEveryPointAmountNonNegative()
    {
        assertTrue(sql.contains("constraint `chk_document_unlock_spent_points` check (`spent_points` >= 0)"));
        assertTrue(sql.contains("constraint `chk_signin_record_awarded_points` check (`awarded_points` >= 0)"));
        assertTrue(sql.contains("constraint `chk_ad_reward_record_awarded_points` check (`awarded_points` >= 0)"));
        assertTrue(sql.contains("constraint `chk_share_task_record_awarded_points` check (`awarded_points` >= 0)"));
        assertTrue(sql.contains("constraint `chk_vip_order_gift_points_snapshot` check (`gift_points_snapshot` >= 0)"));
    }

    @Test
    void allTablesUseRequiredEngineCharsetPrimaryKeyAndAuditColumns()
    {
        String[] definitions = sql.split("create table ");
        assertEquals(29, definitions.length);
        for (int i = 1; i < definitions.length; i++)
        {
            String table = definitions[i].substring(0, definitions[i].indexOf(';'));
            assertTrue(table.contains("`id` bigint not null"));
            assertTrue(table.contains("primary key (`id`)"));
            assertTrue(table.contains("`create_time` datetime not null"));
            assertTrue(table.contains("`update_time` datetime not null"));
            assertTrue(table.contains("`del_flag` char(1) not null default '0'"));
            assertTrue(table.contains("engine=innodb") && table.contains("charset=utf8mb4"));
        }
    }
}
