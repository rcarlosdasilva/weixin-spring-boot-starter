package io.github.rcarlosdasilva.weixin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = WeixinProperties.WEIXIN_PREFIX)
public class WeixinProperties {

  public static final String WEIXIN_PREFIX = "weixin";

  private boolean throwException = false;
  private CacheType cacheType = CacheType.MEMORY;
  private boolean useSpringRedisConfig = false;
  private WeixinRedisProperties redis;

  public boolean isThrowException() {
    return throwException;
  }

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  public CacheType getCacheType() {
    return cacheType;
  }

  public void setCacheType(CacheType cacheType) {
    this.cacheType = cacheType;
  }

  public boolean isUseSpringRedisConfig() {
    return useSpringRedisConfig;
  }

  public void setUseSpringRedisConfig(boolean useSpringRedisConfig) {
    this.useSpringRedisConfig = useSpringRedisConfig;
  }

  public WeixinRedisProperties getRedis() {
    return redis;
  }

  public void setRedis(WeixinRedisProperties redis) {
    this.redis = redis;
  }

}
