package io.github.rcarlosdasilva.weixin;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import com.google.common.base.Preconditions;

import io.github.rcarlosdasilva.weixin.api.Weixin;
import io.github.rcarlosdasilva.weixin.core.WeixinRegistry;
import io.github.rcarlosdasilva.weixin.core.cache.holder.RedisTemplateHandler;
import io.github.rcarlosdasilva.weixin.core.config.RedisConfiguration;
import io.github.rcarlosdasilva.weixin.core.loader.AccountLoader;
import io.github.rcarlosdasilva.weixin.model.Account;
import io.github.rcarlosdasilva.weixin.properties.CacheType;
import io.github.rcarlosdasilva.weixin.properties.WeixinProperties;
import io.github.rcarlosdasilva.weixin.properties.WeixinRedisProperties;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ConditionalOnClass({ WeixinRegistry.class, Weixin.class })
@EnableConfigurationProperties({ WeixinProperties.class })
public class WeixinAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(WeixinAutoConfiguration.class);

  @Autowired(required = false)
  private WeixinProperties weixinProperties;
  @SuppressWarnings("rawtypes")
  @Autowired(required = false)
  private RedisTemplate redisTemplate;
  @Autowired(required = false)
  private AccountLoader accountLoader;

  @PostConstruct
  public void init() {
    if (weixinProperties == null) {
      logger.error("无法加载微信配置");
    }

    config();
    register();
  }

  private void config() {
    io.github.rcarlosdasilva.weixin.core.config.Configuration configuration = new io.github.rcarlosdasilva.weixin.core.config.Configuration();
    configuration.setThrowException(weixinProperties.isThrowException());

    if (weixinProperties.getCacheType() == CacheType.REIDS) {
      configuration.setUseRedisCache(true);

      if (weixinProperties.isUseSpringRedisConfig()) {
        configuration.setUseSpringRedis(true);
        RedisTemplateHandler.redisTemplate = redisTemplate;
      } else {
        RedisConfiguration redisConfiguration = copyReidsConfig();
        configuration.setRedisConfiguration(redisConfiguration);
      }
    }

    WeixinRegistry.config(configuration);
  }

  private RedisConfiguration copyReidsConfig() {
    if (weixinProperties.getRedis() == null) {
      logger.error("无法加载Spring Redis配置");
    }

    RedisConfiguration redisConfiguration = new RedisConfiguration();
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

  public void register() {
    if (accountLoader == null) {
      return;
    }

    Map<String, Account> accountMap = accountLoader.loadAsMap();
    if (accountMap != null && accountMap.size() > 0) {
      Set<String> keys = accountMap.keySet();
      for (String key : keys) {
        registerOne(key, accountMap.get(key));
      }
      return;
    }

    List<Account> accountList = accountLoader.loadAsList();
    if (accountList != null && accountList.size() > 0) {
      for (Account account : accountList) {
        registerOne(account.getAppId(), account);
      }
    }
  }

  private void registerOne(String key, Account account) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(account);

    WeixinRegistry.registry(key, account);
    logger.info("已加载公众号 with key: {}", key);
  }

}
