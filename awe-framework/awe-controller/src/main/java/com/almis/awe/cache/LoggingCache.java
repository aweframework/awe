package com.almis.awe.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

/**
 * Cache wrapper that logs cache hits.
 */
@Slf4j
public class LoggingCache implements Cache {

  public static final String CACHE_HIT_KEY = "Cache hit [{}] key={}";
  private final Cache delegate;
  private final String name;

  public LoggingCache(Cache delegate, String name) {
    this.delegate = delegate;
    this.name = name;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Object getNativeCache() {
    return delegate.getNativeCache();
  }

  @Override
  public ValueWrapper get(Object key) {
    ValueWrapper value = delegate.get(key);
    if (value != null) {
      log.trace(CACHE_HIT_KEY, name, key);
    }
    return value;
  }

  @Override
  public <T> T get(Object key, Class<T> type) {
    T value = delegate.get(key, type);
    if (value != null) {
      log.trace(CACHE_HIT_KEY, name, key);
    }
    return value;
  }

  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    ValueWrapper existing = delegate.get(key);
    if (existing != null) {
      log.trace(CACHE_HIT_KEY, name, key);
      @SuppressWarnings("unchecked")
      T value = (T) existing.get();
      return value;
    }
    return delegate.get(key, valueLoader);
  }

  @Override
  public void put(Object key, Object value) {
    delegate.put(key, value);
  }

  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    return delegate.putIfAbsent(key, value);
  }

  @Override
  public void evict(Object key) {
    delegate.evict(key);
  }

  @Override
  public void clear() {
    delegate.clear();
  }
}
