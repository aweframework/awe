package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StaticResourcesController {

  // Autowired services
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Static resource controller constructor
   *
   * @param baseConfigProperties Base config properties
   */
  public StaticResourcesController(BaseConfigProperties baseConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Handler for index page
   *
   * @return Index page
   */
  @GetMapping(value = {"/", "/screen/**"})
  public String index(Model model) {
    model.addAttribute("faviconIcon", baseConfigProperties.getPaths().getIconFavicon());
    model.addAttribute("phoneIcon", baseConfigProperties.getPaths().getIconPhone());
    model.addAttribute("tabletIcon", baseConfigProperties.getPaths().getIconTablet());
    // An empty value keeps the built-in AWE mark
    String splashLogo = baseConfigProperties.getPaths().getImageSplashLogo();
    model.addAttribute("splashLogo", StringUtils.hasText(splashLogo) ? splashLogo.trim() : null);
    // The light or dark mode of the application paints the splash before the client loads; system adds no class
    String themeMode = baseConfigProperties.getThemeMode();
    model.addAttribute("themeMode", "light".equals(themeMode) || "dark".equals(themeMode) ? themeMode : null);
    return "index";
  }

  /**
   * Parse styles and set image values
   *
   * @param model MVC Model object
   * @return CSS styles
   */
  @GetMapping("/css/styles{xxx}.css")
  public String getStyles(@PathVariable String xxx, Model model) {
    model.addAttribute("startupLogo", baseConfigProperties.getPaths().getImageStartupLogo());
    model.addAttribute("startupBackground", baseConfigProperties.getPaths().getImageStartupBackground());
    model.addAttribute("navbarLogo", baseConfigProperties.getPaths().getImageNavbarLogo());
    return "styles.css";
  }
}
