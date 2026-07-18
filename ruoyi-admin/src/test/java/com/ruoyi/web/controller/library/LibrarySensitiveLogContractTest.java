package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibrarySensitiveLogContractTest
{
    @Test
    void courseCodePlaintextResponseIsNotSavedToOperationLog() throws Exception
    {
        Method method = LibraryCourseCodeController.class.getMethod("generate",
                com.ruoyi.library.dto.CourseCodeGenerateRequest.class);
        Log log = method.getAnnotation(Log.class);

        assertFalse(log.isSaveResponseData());
    }

    @Test
    void refundConfirmationTokenIsExcludedFromOperationLog() throws Exception
    {
        Method method = LibraryVipRefundController.class.getMethod("refund",
                com.ruoyi.library.dto.VipRefundRequest.class);
        Log log = method.getAnnotation(Log.class);

        assertFalse(log.isSaveResponseData());
        assertTrue(Arrays.asList(log.excludeParamNames()).contains("confirmationToken"));
    }
}
