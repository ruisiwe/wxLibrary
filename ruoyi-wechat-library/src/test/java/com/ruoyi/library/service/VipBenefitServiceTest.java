package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.mapper.WlVipBenefitMapper;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipBenefitServiceTest
{
    private WlVipBenefitMapper mapper;
    private VipBenefitService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(WlVipBenefitMapper.class);
        service = new VipBenefitService(mapper);
    }

    @Test
    void rejectsInvalidBenefitFields()
    {
        assertEquals("权益文字不能为空", assertThrows(ServiceException.class,
                () -> service.add(new WlVipBenefit(), "admin")).getMessage());
        assertEquals("权益文字不能超过100个字符", assertThrows(ServiceException.class,
                () -> service.add(benefit(repeat("权", 101), 0, "0"), "admin")).getMessage());
        assertEquals("权益排序不能小于0", assertThrows(ServiceException.class,
                () -> service.add(benefit("赠送积分", -1, "0"), "admin")).getMessage());
        assertEquals("权益状态不正确", assertThrows(ServiceException.class,
                () -> service.add(benefit("赠送积分", 0, "2"), "admin")).getMessage());
    }

    @Test
    void addTrimsTextAndRecordsOperator()
    {
        WlVipBenefit request = benefit("  VIP 文档免费下载  ", null, "0");
        when(mapper.insertBenefit(request)).thenReturn(1);

        assertEquals(1, service.add(request, " admin "));

        verify(mapper).insertBenefit(argThat(benefit ->
                "VIP 文档免费下载".equals(benefit.getBenefitText())
                        && Integer.valueOf(0).equals(benefit.getSortOrder())
                        && "admin".equals(benefit.getCreateBy())));
    }

    @Test
    void editRequiresExistingPositiveId()
    {
        WlVipBenefit request = benefit("赠送积分", 10, "0");

        assertEquals("权益编号不能为空", assertThrows(ServiceException.class,
                () -> service.edit(request, "admin")).getMessage());
    }

    @Test
    void listEnabledDelegatesToDedicatedSortedQuery()
    {
        WlVipBenefit first = benefit("赠送积分", 10, "0");
        WlVipBenefit second = benefit("VIP 文档免费下载", 20, "0");
        when(mapper.selectEnabled()).thenReturn(Arrays.asList(first, second));

        assertSame(first, service.listEnabled().get(0));
        assertSame(second, service.listEnabled().get(1));
    }

    private WlVipBenefit benefit(String text, Integer sortOrder, String status)
    {
        WlVipBenefit benefit = new WlVipBenefit();
        benefit.setBenefitText(text);
        benefit.setSortOrder(sortOrder);
        benefit.setStatus(status);
        return benefit;
    }

    private String repeat(String value, int count)
    {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < count; index++) text.append(value);
        return text.toString();
    }
}
