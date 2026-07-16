package com.ruoyi.library.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatLoginProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.util.StreamUtils;

/** 微信登录凭证交换客户端。 */
@Component
public class WechatCodeClient
{
    private final RestTemplate restTemplate;
    private final WechatLoginProperties properties;

    @Autowired
    public WechatCodeClient(RestTemplateBuilder builder, WechatLoginProperties properties)
    {
        this(builder.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis())).build(), properties);
    }

    public WechatCodeClient(RestTemplate restTemplate, WechatLoginProperties properties)
    {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 使用一次性登录凭证换取微信用户标识。
     *
     * @param code 微信登录凭证
     * @return 微信用户标识
     */
    public String exchange(String code)
    {
        if (isBlank(code))
        {
            throw new ServiceException("微信登录凭证不能为空");
        }
        if (isBlank(properties.getAppId()) || isBlank(properties.getSecret())
                || isBlank(properties.getCode2SessionUrl()))
        {
            throw new ServiceException("微信登录服务尚未完成配置");
        }
        String url = UriComponentsBuilder.fromHttpUrl(properties.getCode2SessionUrl())
                .queryParam("appid", properties.getAppId())
                .queryParam("secret", properties.getSecret())
                .queryParam("js_code", code.trim())
                .queryParam("grant_type", "authorization_code")
                .build().encode().toUriString();
        try
        {
            JSONObject body = requestWithoutSensitiveUrlLogging(URI.create(url));
            if (body == null || body.getIntValue("errcode") != 0 || isBlank(body.getString("openid")))
            {
                throw new ServiceException("微信登录凭证校验失败，请重新登录");
            }
            return body.getString("openid");
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (RuntimeException exception)
        {
            throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
        }
    }

    private JSONObject requestWithoutSensitiveUrlLogging(URI uri)
    {
        ClientHttpResponse response = null;
        try
        {
            // 直接使用请求工厂，避免 RestTemplate 的 DEBUG 日志记录含 code、secret 的完整 URL。
            ClientHttpRequest request = restTemplate.getRequestFactory().createRequest(uri, HttpMethod.GET);
            response = request.execute();
            if (!response.getStatusCode().is2xxSuccessful())
                throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
            return JSON.parseObject(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        }
        catch (IOException exception)
        {
            throw new ServiceException("微信登录服务暂时不可用，请稍后重试");
        }
        finally
        {
            if (response != null) response.close();
        }
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
