package com.almis.awe.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CacheListener {

  private static final List<String> CACHE_NAMES = Arrays.asList("xml", "enumerated", "query", "queue", "maintain",
    "email", "service", "action", "screen", "menu", "profile", "locale", "helpTemplates", "angularTemplates",
    "screenTemplates");
  private final CacheManager cacheManager;

  public CacheListener(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @EventListener
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("Clearing XML and templates from distributed cache on startup...");
    cacheManager.getCacheNames()
      .parallelStream()
      .filter(CACHE_NAMES::contains)
      .map(cacheManager::getCache)
      .filter(Objects::nonNull)
      .forEach(Cache::clear);
  }
}
