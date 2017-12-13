package io.github.rcarlosdasilva.weixin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.github.rcarlosdasilva.weixin.core.setting.Setting;

@ConfigurationProperties(prefix = WeixinProperties.WEIXIN_PREFIX)
public class WeixinProperties {

  public static final String WEIXIN_PREFIX = "weixin";

  private WeixinOpenPlatformProperties openPlatform;
  private Setting setting = new Setting();

  public WeixinOpenPlatformProperties getOpenPlatform() {
    return openPlatform;
  }

  public void setOpenPlatform(WeixinOpenPlatformProperties openPlatform) {
    this.openPlatform = openPlatform;
  }

  public Setting getSetting() {
    return setting;
  }

  public void setSetting(Setting setting) {
    this.setting = setting;
  }

}
