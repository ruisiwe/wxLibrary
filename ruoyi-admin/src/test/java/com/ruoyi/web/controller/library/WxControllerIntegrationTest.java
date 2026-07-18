package com.ruoyi.web.controller.library;

import com.ruoyi.library.auth.WxLoginService;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import com.ruoyi.library.dto.WxProfileResponse;
import com.ruoyi.library.service.WxProfileService;
import com.ruoyi.web.controller.library.wx.WxApiExceptionHandler;
import com.ruoyi.web.controller.library.wx.WxAuthController;
import com.ruoyi.web.controller.library.wx.WxProfileController;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxControllerIntegrationTest
{
    private WxLoginService loginService;
    private WxProfileService profileService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        loginService = mock(WxLoginService.class);
        profileService = mock(WxProfileService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new WxAuthController(loginService), new WxProfileController(profileService))
                .setControllerAdvice(new WxApiExceptionHandler())
                .build();
        WxUserContext.set(7L);
    }

    @AfterEach
    void tearDown()
    {
        WxUserContext.clear();
    }

    @Test
    void uploadFileLoginAcceptsOrdinaryFormFieldsAndAvatar() throws Exception
    {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1});
        when(loginService.login(any(WxLoginRequest.class), any(MultipartFile.class), any()))
                .thenReturn(loginResponse());

        mockMvc.perform(multipart("/wx/auth/login").file(avatar)
                        .param("code", "wx-code")
                        .param("nickname", "微信用户")
                        .param("privacyAccepted", "true")
                        .param("privacyVersion", "p1")
                        .param("statementAccepted", "true")
                        .param("statementVersion", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<WxLoginRequest> request = ArgumentCaptor.forClass(WxLoginRequest.class);
        verify(loginService).login(request.capture(), any(MultipartFile.class), any());
        assertEquals("wx-code", request.getValue().getCode());
        assertEquals("微信用户", request.getValue().getNickname());
        assertEquals("p1", request.getValue().getPrivacyVersion());
    }

    @Test
    void multipartLoginKeepsJsonRequestPartCompatibility() throws Exception
    {
        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json",
                ("{\"code\":\"wx-code\",\"nickname\":\"微信用户\","
                        + "\"privacyAccepted\":true,\"privacyVersion\":\"p1\","
                        + "\"statementAccepted\":true,\"statementVersion\":\"s1\"}")
                        .getBytes(StandardCharsets.UTF_8));
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1});
        when(loginService.login(any(WxLoginRequest.class), any(MultipartFile.class), any()))
                .thenReturn(loginResponse());

        mockMvc.perform(multipart("/wx/auth/login").file(requestPart).file(avatar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<WxLoginRequest> request = ArgumentCaptor.forClass(WxLoginRequest.class);
        verify(loginService).login(request.capture(), any(MultipartFile.class), any());
        assertEquals("wx-code", request.getValue().getCode());
        assertEquals("微信用户", request.getValue().getNickname());
    }

    @Test
    void laterLoginAcceptsJsonWithoutAvatar() throws Exception
    {
        when(loginService.login(any(WxLoginRequest.class), any(), any())).thenReturn(loginResponse());

        mockMvc.perform(post("/wx/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("token"));

        ArgumentCaptor<WxLoginRequest> request = ArgumentCaptor.forClass(WxLoginRequest.class);
        verify(loginService).login(request.capture(), org.mockito.ArgumentMatchers.isNull(), any());
        assertEquals("wx-code", request.getValue().getCode());
    }

    @Test
    void nicknameUpdateUsesJsonPut() throws Exception
    {
        when(profileService.update(7L, "新昵称", null)).thenReturn(profileResponse("新昵称"));

        mockMvc.perform(put("/wx/profile").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"新昵称\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));
    }

    @Test
    void avatarUpdateUsesUploadFileCompatiblePost() throws Exception
    {
        MockMultipartFile avatar = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {1});
        when(profileService.update(anyLong(), any(), any(MultipartFile.class)))
                .thenReturn(profileResponse("新昵称"));

        mockMvc.perform(multipart("/wx/profile/avatar").file(avatar).param("nickname", "新昵称"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("新昵称"));

        verify(profileService).update(7L, "新昵称", avatar);
    }

    @Test
    void avatarUpdateRequiresUploadPart() throws Exception
    {
        mockMvc.perform(multipart("/wx/profile/avatar").param("nickname", "新昵称"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求参数不完整"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unsupportedProfileMediaTypeUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(put("/wx/profile").contentType(MediaType.TEXT_PLAIN).content("nickname"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(41501))
                .andExpect(jsonPath("$.message").value("请求类型不支持"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unsupportedAvatarMediaTypeUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(post("/wx/profile/avatar").contentType(MediaType.TEXT_PLAIN).content("avatar"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(41501))
                .andExpect(jsonPath("$.message").value("请求类型不支持"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void missingMultipartParameterUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(multipart("/wx/auth/login")
                        .file(new MockMultipartFile("avatar", "a.png", "image/png", new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求参数不完整"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void malformedJsonUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(post("/wx/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求内容格式不正确"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void invalidBooleanUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(multipart("/wx/auth/login")
                        .file(new MockMultipartFile("avatar", "a.png", "image/png", new byte[] {1}))
                        .param("code", "code").param("nickname", "用户")
                        .param("privacyAccepted", "not-boolean").param("privacyVersion", "p1")
                        .param("statementAccepted", "true").param("statementVersion", "s1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请求参数格式不正确"));
    }

    @Test
    void unsupportedMediaTypeUsesUnifiedResponse() throws Exception
    {
        mockMvc.perform(post("/wx/auth/login").contentType(MediaType.TEXT_PLAIN).content("code"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("请求类型不支持"));
    }

    @Test
    void uploadLimitUsesUnifiedResponse() throws Exception
    {
        when(loginService.login(any(), any(), any()))
                .thenThrow(new MaxUploadSizeExceededException(1024));

        mockMvc.perform(multipart("/wx/auth/login")
                        .file(new MockMultipartFile("avatar", "a.png", "image/png", new byte[] {1}))
                        .param("code", "code").param("nickname", "用户")
                        .param("privacyAccepted", "true").param("privacyVersion", "p1")
                        .param("statementAccepted", "true").param("statementVersion", "s1"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("上传文件大小超出限制"));
    }

    @Test
    void unknownExceptionDoesNotLeakInternalMessage() throws Exception
    {
        when(loginService.login(any(), any(), any()))
                .thenThrow(new IllegalStateException("database password leaked"));

        String body = mockMvc.perform(post("/wx/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("服务暂时不可用，请稍后重试"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("database password leaked"));
    }

    private WxLoginResponse loginResponse()
    {
        return new WxLoginResponse("token", 7L, "用户", "avatar.png", 0L, false);
    }

    private WxProfileResponse profileResponse(String nickname)
    {
        WlWxUser user = new WlWxUser();
        user.setId(7L);
        user.setNickname(nickname);
        user.setAvatarPath("avatar.png");
        user.setPointBalance(0L);
        return new WxProfileResponse(user);
    }
}
