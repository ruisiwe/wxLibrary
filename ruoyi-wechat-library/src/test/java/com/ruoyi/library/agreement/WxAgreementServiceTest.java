package com.ruoyi.library.agreement;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlAgreement;
import com.ruoyi.library.domain.WlUserAgreement;
import com.ruoyi.library.mapper.WlAgreementMapper;
import com.ruoyi.library.mapper.WlUserAgreementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxAgreementServiceTest
{
    private WlAgreementMapper agreementMapper;
    private WlUserAgreementMapper userAgreementMapper;
    private WxAgreementService service;

    @BeforeEach
    void setUp()
    {
        agreementMapper = mock(WlAgreementMapper.class);
        userAgreementMapper = mock(WlUserAgreementMapper.class);
        service = new WxAgreementService(agreementMapper, userAgreementMapper);
        when(agreementMapper.selectCurrentByType(WxAgreementService.TYPE_PRIVACY))
                .thenReturn(agreement(1L, "PRIVACY", "p1"));
        when(agreementMapper.selectCurrentByType(WxAgreementService.TYPE_STATEMENT))
                .thenReturn(agreement(2L, "STATEMENT", "s1"));
    }

    @Test
    void firstAcceptanceReportsEachMissingItemInChinese()
    {
        assertEquals("请勾选用户隐私协议", assertThrows(ServiceException.class,
                () -> service.validateCurrentAcceptance(false, "p1", true, "s1")).getMessage());
        assertEquals("请提交当前用户隐私协议版本", assertThrows(ServiceException.class,
                () -> service.validateCurrentAcceptance(true, "old", true, "s1")).getMessage());
        assertEquals("请勾选网站声明", assertThrows(ServiceException.class,
                () -> service.validateCurrentAcceptance(true, "p1", false, "s1")).getMessage());
        assertEquals("请提交当前网站声明版本", assertThrows(ServiceException.class,
                () -> service.validateCurrentAcceptance(true, "p1", true, "old")).getMessage());
    }

    @Test
    void acceptsBothCurrentVersionsAndIsIdempotent()
    {
        service.acceptCurrent(8L, "p1", "s1", "127.0.0.1");
        verify(userAgreementMapper).insertUserAgreement(org.mockito.ArgumentMatchers.argThat(
                item -> item.getUserId().equals(8L) && item.getAgreementId().equals(1L)));
        verify(userAgreementMapper).insertUserAgreement(org.mockito.ArgumentMatchers.argThat(
                item -> item.getUserId().equals(8L) && item.getAgreementId().equals(2L)));

        when(userAgreementMapper.selectByUserAndAgreement(8L, 1L)).thenReturn(new WlUserAgreement());
        when(userAgreementMapper.selectByUserAndAgreement(8L, 2L)).thenReturn(new WlUserAgreement());
        service.acceptCurrent(8L, "p1", "s1", "127.0.0.1");
        verify(userAgreementMapper, org.mockito.Mockito.times(2)).insertUserAgreement(any(WlUserAgreement.class));
    }

    @Test
    void reportsWhetherUserAcceptedBothPublishedVersions()
    {
        when(userAgreementMapper.countAcceptedAgreementIds(8L, 1L, 2L)).thenReturn(2);
        assertTrue(service.hasAcceptedAllCurrent(8L));
    }

    @Test
    void publishingNewVersionDisablesOldVersionThenActivatesDraft()
    {
        WlAgreement draft = agreement(3L, "PRIVACY", "p2");
        draft.setStatus(WxAgreementService.STATUS_DRAFT);
        when(agreementMapper.selectAgreementByIdForUpdate(3L)).thenReturn(draft);
        when(agreementMapper.publishAgreement(3L, "admin")).thenReturn(1);

        service.publish(3L, "admin");

        verify(agreementMapper).disablePublishedByType("PRIVACY", 3L, "admin");
        verify(agreementMapper).publishAgreement(3L, "admin");
    }

    @Test
    void publishedAgreementCannotBeEditedAsDraft()
    {
        WlAgreement published = agreement(3L, "PRIVACY", "p2");
        published.setStatus(WxAgreementService.STATUS_PUBLISHED);
        when(agreementMapper.selectAgreementById(3L)).thenReturn(published);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.updateDraft(published));
        assertEquals("仅草稿协议允许编辑", exception.getMessage());
        verify(agreementMapper, never()).updateDraft(any(WlAgreement.class));
    }

    private WlAgreement agreement(Long id, String type, String version)
    {
        WlAgreement agreement = new WlAgreement();
        agreement.setId(id);
        agreement.setAgreementType(type);
        agreement.setVersion(version);
        agreement.setTitle(type);
        agreement.setContent("内容");
        agreement.setStatus(WxAgreementService.STATUS_PUBLISHED);
        return agreement;
    }
}
