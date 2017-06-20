package io.github.rcarlosdasilva.weixin;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import io.github.rcarlosdasilva.weixin.api.Weixin;
import io.github.rcarlosdasilva.weixin.core.WeixinRegistry;
import io.github.rcarlosdasilva.weixin.core.cache.holder.RedisTemplateHandler;
import io.github.rcarlosdasilva.weixin.core.handler.NotificationHandler;
import io.github.rcarlosdasilva.weixin.core.handler.NotificationHandlerProxy;
import io.github.rcarlosdasilva.weixin.core.listener.AccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.JsTicketUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.OpenPlatformAccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.listener.OpenPlatformLisensorAccessTokenUpdatedListener;
import io.github.rcarlosdasilva.weixin.core.registry.RedisSetting;
import io.github.rcarlosdasilva.weixin.core.registry.Setting;
import io.github.rcarlosdasilva.weixin.extension.AccountLoader;
import io.github.rcarlosdasilva.weixin.model.Account;
import io.github.rcarlosdasilva.weixin.properties.CacheType;
import io.github.rcarlosdasilva.weixin.properties.WeixinOpenPlatformProperties;
import io.github.rcarlosdasilva.weixin.properties.WeixinProperties;
import io.github.rcarlosdasilva.weixin.properties.WeixinRedisProperties;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ConditionalOnClass({ WeixinRegistry.class, Weixin.class })
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

    logger.info("Weixin Auto Configuration [微信相关自动配置完毕]");
  }

  private void config() {
    logger.debug("Weixin Config Doing [开始配置微信相关参数]");

    Setting setting = new Setting();

    configBasic(setting);
    configListener(setting);
    configOpenPlatform();
    configNotificationHandler();

    WeixinRegistry.withSetting(setting);

    logger.info("Weixin Config Done [微信相关参数已配置完毕]");
  }

  private void configBasic(Setting setting) {
    final boolean throwException = weixinProperties.isThrowException();
    setting.setThrowException(throwException);
    logger.debug("Weixin Config [微信接口调用错误时，转为异常抛出]: {}", throwException);

    final CacheType cacheType = weixinProperties.getCacheType();
    logger.debug("Weixin Config [缓存方式]: {}", cacheType);
    if (cacheType == CacheType.REIDS) {
      setting.setUseRedisCache(true);

      final boolean useSpringRedis = weixinProperties.isUseSpringRedisConfig();
      logger.debug("Weixin Config [使用Spring配置好的Redis缓存]: {}", useSpringRedis);
      if (useSpringRedis) {
        setting.setUseSpringRedis(true);
        RedisTemplateHandler.redisTemplate = redisTemplate;
      } else {
        setting.setRedisSetting(copyReidsSetting());
      }
    }
  }

  private void configListener(Setting setting) {
    if (accessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册AccessTokenUpdatedListener]");
      setting.addListener(accessTokenUpdatedListener);
    }
    if (jsTicketUpdatedListener != null) {
      logger.debug("Weixin Config [注册JsTicketUpdatedListener]");
      setting.addListener(jsTicketUpdatedListener);
    }
    if (openPlatformAccessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册OpenPlatformAccessTokenUpdatedListener]");
      setting.addListener(openPlatformAccessTokenUpdatedListener);
    }
    if (openPlatformLisensorAccessTokenUpdatedListener != null) {
      logger.debug("Weixin Config [注册OpenPlatformLisensorAccessTokenUpdatedListener]");
      setting.addListener(openPlatformLisensorAccessTokenUpdatedListener);
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
        WeixinRegistry.openPlatform(appId, appSecret, aesToken, aesKey);
      }
    }
  }

  private void configNotificationHandler() {
    if (notificationHandler != null) {
      logger.debug("Weixin Config [微信通知处理器NotificationHandler]: {}" + notificationHandler);
      NotificationHandlerProxy.proxy(notificationHandler);
    } else {
      logger.warn("Weixin Config [没有找到有效的微信通知处理器NotificationHandler，可能会无法处理微信通知]");
    }
  }

  private RedisSetting copyReidsSetting() {
    if (weixinProperties.getRedis() == null) {
      logger.error("无法加载Spring Redis配置");
    }

    RedisSetting redisConfiguration = new RedisSetting();
    WeixinRedisProperties wrp = weixinProperties.getRedis();
    redisConfiguration.setHost(wrp.getHost());
    redisConfiguration.setPort(wrp.getPort());
    redisConfiguration.setPassword(wrp.getPassword());
    redisConfiguration.setDatabase(wrp.getDatabase());
    redisConfiguration.setTimeout(wrp.getTimeout());
    redisConfiguration.setUseSsl(wrp.isSsl());

    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxIdle(wrp.getPool().getMaxIdle());
    poolConfig.setMaxTotal(wrp.getPool().getMaxActive());
    poolConfig.setMaxWaitMillis(wrp.getPool().getMaxWait());
    poolConfig.setMinIdle(wrp.getPool().getMinIdle());
    redisConfiguration.setConfig(poolConfig);

    return redisConfiguration;
  }

  public void loadWeixin() {
    logger.debug("Weixin Account Doing [开始加载微信公众号数据]");

    if (accountLoader == null) {
      logger.warn("Weixin Account [未找到公众号数据加载器AccountLoader]}");
      return;
    }

    final Map<String, Account> accountMap = accountLoader.loadAsMap();
    final boolean validMap = accountMap != null && !accountMap.isEmpty();
    if (validMap) {
      for (String key : accountMap.keySet()) {
        registerOne(key, accountMap.get(key));
      }
    } else {
      logger.warn("Weixin Account [未找到任何公众号数据可加载]");
    }

    logger.info("Weixin Account Done [微信公众号数据加载完毕]");
  }

  private void registerOne(String key, Account account) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(account);

    WeixinRegistry.register(key, account);
    logger.info("Weixin Account [加载 ]: {}", key);
  }

}
