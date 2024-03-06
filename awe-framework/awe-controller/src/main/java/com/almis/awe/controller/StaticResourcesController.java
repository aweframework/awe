package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.Theme;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class StaticResourcesController {

  // Autowired services
  private final BaseConfigProperties baseConfigProperties;
  private final QueryService queryService;

  /**
   * Static resource controller constructor
   *
   * @param baseConfigProperties Base config properties
   * @param queryService         Query service
   */
  public StaticResourcesController(BaseConfigProperties baseConfigProperties, QueryService queryService) {
    this.baseConfigProperties = baseConfigProperties;
    this.queryService = queryService;
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
    return "index";
  }

  /**
   * Parse styles and set image values
   *
   * @param model MVC Model object
   * @return CSS styles
   */
  @GetMapping("/css/styles{xxx}.css")
  public String getStyles(Model model) {
    model.addAttribute("startupLogo", baseConfigProperties.getPaths().getImageStartupLogo());
    model.addAttribute("startupBackground", baseConfigProperties.getPaths().getImageStartupBackground());
    model.addAttribute("navbarLogo", baseConfigProperties.getPaths().getImageNavbarLogo());
    return "styles.css";
  }

  /**
   * Generate theme variables
   *
   * @return CSS styles
   */
  @GetMapping("/css/themeVariables{xxx}.css")
  @ResponseBody
  public String getThemeVariables() throws AWException {
    // Call themes query and retrieve all variables
    DataList dataList = queryService.launchPrivateQuery("themeVariables").getDataList();
    List<Theme> themeList = DataListUtil.asBeanList(dataList, Theme.class);

    return themeList.stream().map(Theme::toString).collect(Collectors.joining("\n"));
  }
}

