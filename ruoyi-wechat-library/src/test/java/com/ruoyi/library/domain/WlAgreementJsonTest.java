package com.ruoyi.library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.TimeZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WlAgreementJsonTest
{
    @Test
    void deserializesRuoyiDateTimeStringForEffectiveTime() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setTimeZone(TimeZone.getDefault());
        WlAgreement agreement = objectMapper.readValue("{\"effectiveTime\":\"2026-07-23 14:45:48\"}",
                WlAgreement.class);

        assertEquals("2026-07-23 14:45:48",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(agreement.getEffectiveTime()));
    }
}
