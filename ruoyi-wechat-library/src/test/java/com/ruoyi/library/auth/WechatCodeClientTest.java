package com.ruoyi.library.auth;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatLoginProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class WechatCodeClientTest
{
    private MockRestServiceServer server;
    private WechatCodeClient client;

    @BeforeEach
    void setUp()
    {
        WechatLoginProperties properties = new WechatLoginProperties();
        properties.setAppId("test-app");
        properties.setSecret("test-secret");
        properties.setCode2SessionUrl("https://api.weixin.qq.com/sns/jscode2session");
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new WechatCodeClient(restTemplate, properties);
    }

    @Test
    void exchangesCodeForOpenid()
    {
        server.expect(method(GET))
                .andExpect(queryParam("appid", "test-app"))
                .andExpect(queryParam("secret", "test-secret"))
                .andExpect(queryParam("js_code", "login-code"))
                .andExpect(queryParam("grant_type", "authorization_code"))
                .andRespond(withSuccess("{\"openid\":\"wx-openid\"}", MediaType.APPLICATION_JSON));

        assertEquals("wx-openid", client.exchange("login-code"));
        server.verify();
    }

    @Test
    void rejectsBlankCodeWithoutNetworkCall()
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> client.exchange(" "));
        assertEquals("微信登录凭证不能为空", exception.getMessage());
        server.verify();
    }

    @Test
    void convertsWechatBusinessErrorToChineseMessage()
    {
        server.expect(method(GET)).andRespond(withSuccess(
                "{\"errcode\":40029,\"errmsg\":\"invalid code\"}", MediaType.APPLICATION_JSON));

        ServiceException exception = assertThrows(ServiceException.class, () -> client.exchange("bad-code"));
        assertEquals("微信登录凭证校验失败，请重新登录", exception.getMessage());
    }
}
