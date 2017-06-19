package io.github.rcarlosdasilva.weixin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = WeixinOpenPlatformProperties.WEIXIN_OPEN_PLATFORM_PREFIX)
public class WeixinOpenPlatformProperties {

  public static final String WEIXIN_OPEN_PLATFORM_PREFIX = "weixin.open-platform";

  private String appId;
  private String appSecret;
  private String aesToken;
  private String aesKey;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getAppSecret() {
    return appSecret;
  }

  public void setAppSecret(String appSecret) {
    this.appSecret = appSecret;
  }

  public String getAesToken() {
    return aesToken;
  }

  public void setAesToken(String aesToken) {
    this.aesToken = aesToken;
  }

  public String getAesKey() {
    return aesKey;
  }

  public void setAesKey(String aesKey) {
    this.aesKey = aesKey;
  }

}
