package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class StaticResourcesController {

  // The theme draws the AWE mark itself, with the AWE wordmark beside it
  private static final String AWE_MARK_LOGO = "awe-logo-mark.svg";

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
    // An empty value keeps the built-in brand: the AWE mark with its wordmark
    String navbarLogo = baseConfigProperties.getPaths().getImageNavbarLogo();
    model.addAttribute("navbarLogo", isApplicationBrand(navbarLogo) ? navbarLogo.trim() : null);
    // The logo of the collapsed navigation bar; unset, the navigation bar logo is fitted in the rail
    String navbarRailLogo = baseConfigProperties.getPaths().getImageNavbarLogoCollapsed();
    model.addAttribute("navbarRailLogo", StringUtils.hasText(navbarRailLogo) ? navbarRailLogo.trim() : null);
    // The light or dark mode of the application paints the splash before the client loads; system adds no class
    String themeMode = baseConfigProperties.getThemeMode();
    model.addAttribute("themeMode", "light".equals(themeMode) || "dark".equals(themeMode) ? themeMode : null);
    return "index";
  }

  /**
   * Whether the navigation bar shows a logo of the application instead of the built-in AWE brand. The theme paints the
   * AWE mark with the AWE wordmark beside it, and any other logo alone, as the AngularJS engine does
   *
   * @param navbarLogo Configured navigation bar logo
   * @return The logo is a mark of the application
   */
  private boolean isApplicationBrand(String navbarLogo) {
    return StringUtils.hasText(navbarLogo) && !navbarLogo.trim().endsWith(AWE_MARK_LOGO);
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
