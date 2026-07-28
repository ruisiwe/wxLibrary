package com.ruoyi.library.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WlAgreementJsonTest
{
    @Test
    void deserializesRuoyiDateTimeStringForEffectiveTime() throws Exception
    {
        WlAgreement agreement = new ObjectMapper().readValue("{\"effectiveTime\":\"2026-07-23 14:45:48\"}",
                WlAgreement.class);

        assertEquals("2026-07-23 14:45:48",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(agreement.getEffectiveTime()));
    }
}
