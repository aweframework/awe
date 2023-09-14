package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.settings.WebSettings;
import com.almis.awe.service.InitService;
import com.almis.awe.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manage settings request
 */
@RestController
@RequestMapping("/settings")
public class SettingsController extends ServiceConfig {

  // Autowired services
  private final MenuService menuService;
  private final InitService initService;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Initialize controller
   * @param menuService Menu service
   * @param initService Init service
   * @param baseConfigProperties Base properties
   */
  @Autowired
  public SettingsController(MenuService menuService, InitService initService, BaseConfigProperties baseConfigProperties) {
    this.menuService = menuService;
    this.initService = initService;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve application settings
   *
   * @param httpServletRequest Servlet request
   * @return WebSettings
   * @throws AWException Error generating settings
   */
  @PostMapping
  public WebSettings getSettings(HttpServletRequest httpServletRequest) throws AWException {
    WebSettings settings = getBean(WebSettings.class).toBuilder().build();

    // Launch client start service
    initService.onClientStart();

    // Overwrite settings parameters
    overwriteSettingParameters(settings);

    // Handle initial redirection
    handleInitialRedirection(settings, httpServletRequest);

    // Retrieve settings
    return settings;
  }

  /**
   * Overwrite setting parameters
   *
   * @param settings Settings
   */
  private void overwriteSettingParameters(WebSettings settings) {
    if (getSession().getParameter(String.class, AweConstants.SESSION_LANGUAGE) != null) {
      settings.setLanguage(getSession().getParameter(String.class, AweConstants.SESSION_LANGUAGE));
    } else {
      settings.setLanguage(baseConfigProperties.getLanguageDefault());
    }
    if (getSession().getParameter(String.class, AweConstants.SESSION_THEME) != null) {
      settings.setTheme(getSession().getParameter(String.class, AweConstants.SESSION_THEME));
    } else {
      settings.setTheme(baseConfigProperties.getTheme());
    }
    if (getSession().getParameter(String.class, AweConstants.SESSION_INITIAL_URL) != null) {
      settings.setInitialURL(getSession().getParameter(String.class, AweConstants.SESSION_INITIAL_URL));
    } else {
      settings.setInitialURL(baseConfigProperties.getPaths().getServer());
    }
    settings.setCometUID(getRequest().getToken());
  }

  /**
   * Handle initial redirection
   *
   * @param settings Web settings
   * @param request  Request
   * @throws AWException Error handling redirection
   */
  private void handleInitialRedirection(WebSettings settings, HttpServletRequest request) throws AWException {
    String referer = request.getHeader("referer");
    String origin = request.getHeader("origin");
    String basePath = origin + request.getContextPath() + AweConstants.FILE_SEPARATOR;
    if (referer != null && origin != null && referer.startsWith(basePath)) {
      String address = referer.replace(basePath, "");
      settings.setReloadCurrentScreen(!menuService.checkOptionAddress(address));
    } else {
      settings.setReloadCurrentScreen(false);
    }
  }
}
