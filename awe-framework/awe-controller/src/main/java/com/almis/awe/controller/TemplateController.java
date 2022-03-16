package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.HelpService;
import com.almis.awe.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;

/**
 * Manage template request
 */
@Controller
@RequestMapping("/template")
@Slf4j
public class TemplateController {

  // Autowired services
  private final TemplateService templateService;
  private final HelpService helpService;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param templateService      Template service
   * @param helpService          Help service
   * @param baseConfigProperties Base config properties
   */
  @Autowired
  public TemplateController(TemplateService templateService, HelpService helpService, BaseConfigProperties baseConfigProperties) {
    this.templateService = templateService;
    this.helpService = helpService;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve angular templates
   *
   * @param template Angular template
   * @return Angular template
   */
  @GetMapping("/angular/{template}")
  public String getAngularTemplate(@PathVariable String template) {
    return Paths.get(baseConfigProperties.getPaths().getTemplatesAngular(), template).toString();
  }

  /**
   * Retrieve angular module templates
   *
   * @param module   Angular module
   * @param template Angular template
   * @return Angular template
   */
  @GetMapping("/angular/{module}/{template}")
  public String getAngularSubTemplate(@PathVariable String module, @PathVariable String template) {
    return Paths.get(baseConfigProperties.getPaths().getTemplatesAngular(), module, template).toString();
  }

  /**
   * Retrieve screen templates
   *
   * @param view     Screen view
   * @param optionId Option identifier
   * @return Screen template
   */
  @GetMapping("/screen/{view}/{optionId}")
  public @ResponseBody
  String getScreenTemplate(@PathVariable String view, @PathVariable String optionId) throws AWException {
    return templateService.getTemplate(view, optionId);
  }

  /**
   * Retrieve default screen template
   *
   * @return Screen template
   */
  @GetMapping("/screen")
  public @ResponseBody
  String getDefaultScreenTemplate() throws AWException {
    return templateService.getTemplate();
  }

  /**
   * Retrieve application help
   *
   * @return Application help
   */
  @GetMapping("/help")
  public @ResponseBody
  String getApplicationHelp() {
    return helpService.getApplicationHelp();
  }

  /**
   * Retrieve option help
   *
   * @param option Option
   * @return Option help
   */
  @GetMapping("/help/{option}")
  public @ResponseBody
  String getOptionHelp(@PathVariable String option) {
    return helpService.getOptionHelp(option);
  }

  /**
   * Handle error
   *
   * @param exc Exception to handle
   * @return Response error
   */
  @ExceptionHandler(AWException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public @ResponseBody
  String handleAWException(AWException exc) {
    log.error(exc.getTitle() + "\n" + exc.getMessage(), exc);
    return templateService.generateErrorTemplate(exc);
  }
}
