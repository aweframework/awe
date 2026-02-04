package com.almis.awe.cache;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wrap the CacheManager to log cache hits for queryData cache.
 */
@Configuration
public class CacheLoggingConfig {

  @Bean
  public static BeanPostProcessor cacheLoggingPostProcessor() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CacheManager && !(bean instanceof LoggingCacheManager)) {
          return new LoggingCacheManager((CacheManager) bean, "queryData");
        }
        return bean;
      }
    };
  }
}
