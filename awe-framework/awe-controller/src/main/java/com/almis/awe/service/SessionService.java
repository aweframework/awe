package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.component.AweSession;
import lombok.extern.slf4j.Slf4j;

import static com.almis.awe.model.constant.AweConstants.SESSION_MODULE;

/**
 * Session service
 *
 * @author pgarcia
 */
@Slf4j
public class SessionService extends ServiceConfig {

  private final MenuService menuService;

  public SessionService(MenuService menuService) {
    this.menuService = menuService;
  }

  /**
   * Retrieve session parameter
   *
   * @param name Parameter name
   * @return Session parameter
   */
  public Object getSessionParameter(String name) {
    AweSession session = getSession();
    if (isSessionValid(session)) {
      return session.getParameter(name);
    } else {
      return null;
    }
  }

  /**
   * Store session parameter (as string)
   *
   * @param name  Parameter name
   * @param value Parameter value
   */
  public void setSessionParameter(String name, String value) {
    AweSession session = getSession();
    if (isSessionValid(session)) {
      session.setParameter(name, value);

      // If session is module, update menu screens
      if (SESSION_MODULE.equalsIgnoreCase(name)) {
        menuService.regenerateMenuScreens();
      }
    }
  }

  private boolean isSessionValid(AweSession session) {
    return session != null;
  }
}
