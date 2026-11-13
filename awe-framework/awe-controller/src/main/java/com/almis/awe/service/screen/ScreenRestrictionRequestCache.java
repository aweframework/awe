package com.almis.awe.service.screen;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ScreenRestriction;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the screen restrictions read in the current request, so a request reads them from the database only once
 * for the same user, profile and restriction. It lives as long as the request, so there is nothing to invalidate
 * between requests. Outside a request, or once the request has finished, it keeps nothing.
 */
public final class ScreenRestrictionRequestCache {

  private static final String ATTRIBUTE = ScreenRestrictionRequestCache.class.getName();

  private ScreenRestrictionRequestCache() {
  }

  /**
   * Loads the screen restrictions
   */
  @FunctionalInterface
  public interface Loader {
    /**
     * Load the screen restrictions
     *
     * @return Screen restrictions
     * @throws AWException Error loading them
     */
    List<ScreenRestriction> load() throws AWException;
  }

  /**
   * Retrieve the screen restrictions read in the current request, loading them the first time
   *
   * @param key    Key of the restrictions (user, profile and restriction)
   * @param loader Loads the restrictions from the database
   * @return Screen restrictions (read only)
   * @throws AWException Error loading them
   */
  public static List<ScreenRestriction> get(String key, Loader loader) throws AWException {
    Map<String, List<ScreenRestriction>> restrictionsByKey = getRequestMap();
    if (restrictionsByKey == null) {
      return loader.load();
    }

    List<ScreenRestriction> restrictions = restrictionsByKey.get(key);
    if (restrictions == null) {
      restrictions = List.copyOf(loader.load());
      restrictionsByKey.put(key, restrictions);
    }
    return restrictions;
  }

  /**
   * Forget the screen restrictions read in the current request (they may have changed)
   */
  public static void clear() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      try {
        attributes.removeAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      } catch (IllegalStateException exc) {
        // Request already finished: nothing kept
      }
    }
  }

  /**
   * Retrieve the restrictions map of the current request. Asynchronous tasks share the request attributes,
   * so the map supports concurrent access
   *
   * @return Restrictions map, or null without an active request
   */
  @SuppressWarnings("unchecked")
  private static Map<String, List<ScreenRestriction>> getRequestMap() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return null;
    }
    try {
      synchronized (attributes) {
        Map<String, List<ScreenRestriction>> restrictionsByKey =
          (Map<String, List<ScreenRestriction>>) attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (restrictionsByKey == null) {
          restrictionsByKey = new ConcurrentHashMap<>();
          attributes.setAttribute(ATTRIBUTE, restrictionsByKey, RequestAttributes.SCOPE_REQUEST);
        }
        return restrictionsByKey;
      }
    } catch (IllegalStateException exc) {
      // Request already finished (an asynchronous task outlived it): keep nothing
      return null;
    }
  }
}
