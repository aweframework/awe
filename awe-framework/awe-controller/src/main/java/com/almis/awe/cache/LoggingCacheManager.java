package com.almis.awe.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheManager wrapper that logs cache hits for a target cache.
 */
public class LoggingCacheManager implements CacheManager {

  private final CacheManager delegate;
  private final String cacheNameToLog;
  private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

  public LoggingCacheManager(CacheManager delegate, String cacheNameToLog) {
    this.delegate = delegate;
    this.cacheNameToLog = cacheNameToLog;
  }

  @Override
  public Cache getCache(String name) {
    Cache cache = delegate.getCache(name);
    if (cache == null) {
      return null;
    }
    if (!cacheNameToLog.equals(name)) {
      return cache;
    }
    return cacheMap.computeIfAbsent(name, key -> new LoggingCache(cache, name));
  }

  @Override
  public Collection<String> getCacheNames() {
    return delegate.getCacheNames();
  }
}
