package io.github.rcarlosdasilva.weixin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.common.base.Strings;

import io.github.rcarlosdasilva.weixin.core.Registry;
import io.github.rcarlosdasilva.weixin.core.Weixin;
import io.github.rcarlosdasilva.weixin.core.cache.CacheType;
import io.github.rcarlosdasilva.weixin.core.cache.storage.redis.RedisHandler;
import io.github.rcarlosdasilva.weixin.core.handler.NotificationHandler;
import io.github.rcarlosdasilva.weixin.core.handler.NotificationHandlerProxy;
import io.github.rcarlosdasilva.weixin.core.inspect.InspectDispatcher;
import io.github.rcarlosdasilva.weixin.core.listener.AccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.JsTicketUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.OpenPlatformAccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.OpenPlatformLisensorAccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.setting.Setting;
import io.github.rcarlosdasilva.weixin.extension.AccountLoader;
import io.github.rcarlosdasilva.weixin.model.WeixinAccount;
import io.github.rcarlosdasilva.weixin.properties.WeixinOpenPlatformProperties;
import io.github.rcarlosdasilva.weixin.properties.WeixinProperties;

@Configuration
@ConditionalOnClass({ Registry.class, Weixin.class })
@EnableConfigurationProperties({ WeixinProperties.class })
public class WeixinAutoConfiguration implements SmartInitializingSingleton {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired(required = false)
  private WeixinProperties weixinProperties;
  @SuppressWarnings("rawtypes")
  @Autowired(required = false)
  private RedisTemplate redisTemplate;
  @Autowired(required = false)
  private NotificationHandler notificationHandler;
  @Autowired(required = false)
  private AccountLoader accountLoader;

  @Autowired(required = false)
  private AccessTokenUpdatedListener accessTokenUpdatedListener;
  @Autowired(required = false)
  private JsTicketUpdatedListener jsTicketUpdatedListener;
  @Autowired(required = false)
  private OpenPlatformAccessTokenUpdatedListener openPlatformAccessTokenUpdatedListener;
  @Autowired(required = false)
  private OpenPlatformLisensorAccessTokenUpdatedListener openPlatformLisensorAccessTokenUpdatedListener;

  @Override
  public void afterSingletonsInstantiated() {
    if (weixinProperties == null) {
      logger.warn("Weixin Auto Configuration [没有找到有效的微信参数配置，将使用一个默认的配置]");
      weixinProperties = new WeixinProperties();
    }

    config();
    loadWeixin();
    InspectDispatcher.startup();

    logger.info("Weixin Auto Configuration [微信相关自动配置完毕]");
  }

  private void config() {
    logger.debug("Weixin Config Doing [开始配置微信相关参数]");

    Setting setting = weixinProperties.getSetting();
    basic(setting);
    configListener();
    configOpenPlatform();
    configNotificationHandler();

    Registry.withSetting(setting);

    logger.info("Weixin Config Done [微信相关参数已配置完毕]");
  }

  private void basic(Setting setting) {
    logger.debug("Weixin Config [微信接口调用错误时，转为异常抛出]: {}", setting.isThrowException());

    logger.debug("Weixin Config [授权成功后(POST)自动获取公众号信息]: {}",
        setting.isAutoLoadAuthorizedWeixinData());

    logger.debug("Weixin Config [缓存方式]: {}", setting.getCacheType());
    if (setting.getCacheType() == CacheType.SPRING_REDIS && redisTemplate != null) {
      RedisHandler.setRedisTemplate(redisTemplate);
    }
  }

  private void configListener() {
    if (accessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册AccessTokenUpdatedListener]");
      Registry.listener(accessTokenUpdatedListener);
    }
    if (jsTicketUpdatedListener != null) {
      logger.debug("Weixin Config [注册JsTicketUpdatedListener]");
      Registry.listener(jsTicketUpdatedListener);
    }
    if (openPlatformAccessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册OpenPlatformAccessTokenUpdatedListener]");
      Registry.listener(openPlatformAccessTokenUpdatedListener);
    }
    if (openPlatformLisensorAccessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册OpenPlatformLisensorAccessTokenUpdatedListener]");
      Registry.listener(openPlatformLisensorAccessTokenUpdatedListener);
    }
  }

  private void configOpenPlatform() {
    WeixinOpenPlatformProperties openPlatformProperties = weixinProperties.getOpenPlatform();
    if (openPlatformProperties != null) {
      logger.debug("Weixin Config [开放平台相关参数]: ");
      final String appId = openPlatformProperties.getAppId();
      final String appSecret = openPlatformProperties.getAppSecret();
      final String aesToken = openPlatformProperties.getAesToken();
      final String aesKey = openPlatformProperties.getAesKey();

      if (Strings.isNullOrEmpty(appId) || Strings.isNullOrEmpty(appSecret)
          || Strings.isNullOrEmpty(aesToken) || Strings.isNullOrEmpty(aesKey)) {
        logger.warn("Weixin Config [开放平台参数不全，将无法使用开放平台相关功能]");
      } else {
        Registry.openPlatform(appId, appSecret, aesToken, aesKey);
      }
    }
  }

  private void configNotificationHandler() {
    if (notificationHandler != null) {
      logger.debug("Weixin Config [微信通知处理器NotificationHandler]: {}", notificationHandler);
      NotificationHandlerProxy.proxy(notificationHandler);
    } else {
      logger.warn("Weixin Config [没有找到有效的微信通知处理器NotificationHandler，可能会无法处理微信通知]");
    }
  }

  public void loadWeixin() {
    logger.debug("Weixin Account Doing [开始加载微信公众号数据]");

    if (accountLoader == null) {
      logger.warn("Weixin Account [未找到公众号数据加载器AccountLoader]}");
      return;
    }

    final List<WeixinAccount> accounts = accountLoader.load();
    if (accounts == null || accounts.isEmpty()) {
      logger.warn("Weixin Account [未找到任何公众号数据可加载]");
      return;
    }

    for (WeixinAccount account : accounts) {
      Registry.checkin(account);
    }

    logger.info("Weixin Account Done [微信公众号数据加载完毕]");
  }

}
