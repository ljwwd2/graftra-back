package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WeChat OAuth configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    /**
     * WeChat App ID
     */
    private String appId;

    /**
     * WeChat App Secret
     */
    private String appSecret;

    /**
     * WeChat OAuth redirect URI
     */
    private String redirectUri;

    /**
     * WeChat token endpoint
     */
    private String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * WeChat user info endpoint
     */
    private String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";
}
