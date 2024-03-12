package com.almis.awe.controller;

import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.ScreenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Manage all incoming action requests
 */
@RestController
@RequestMapping("/screen-data")
@Slf4j
public class ScreenDataController {

  // Autowired services
  private final ScreenService screenService;
  private final AweRequest request;

  /**
   * Autowired constructor
   *
   * @param screenService Screen service
   * @param aweRequest    Awe request
   */
  @Autowired
  public ScreenDataController(ScreenService screenService, AweRequest aweRequest) {
    this.screenService = screenService;
    this.request = aweRequest;
  }

  /**
   * Retrieve screen data
   *
   * @param parameters Parameters
   * @return Client action list
   */
  @PostMapping
  public ScreenData getDefaultScreenData(@RequestBody ObjectNode parameters) throws AWException {

    // Initialize parameters
    request.setParameterList(parameters);

    // Launch action
    return screenService.getScreenData(generateTemplate());
  }

  /**
   * Retrieve screen data
   *
   * @param optionId   Option identifier
   * @param parameters Parameters
   * @return Client action list
   */
  @PostMapping("/{optionId}")
  public ScreenData getOptionScreenData(@PathVariable("optionId") String optionId,
                                        @RequestBody ObjectNode parameters) throws AWException {

    // Initialize parameters
    request.setParameterList(parameters);

    // Launch action
    return screenService.getScreenData(optionId, generateTemplate());
  }

  /**
   * Check template parameter
   * @return Generate template or not
   */
  private boolean generateTemplate() {
    return Optional.ofNullable(request.getParameter("template")).map(JsonNode::asBoolean).orElse(true);
  }

  /**
   * Handle error
   *
   * @param exc Exception to handle
   */
  @ExceptionHandler(AWException.class)
  public ScreenData handleAWException(AWException exc) {
    log.error("Error generating screen: Internal server error", exc);
    return screenService.getErrorScreenData("error-5xx") ;
  }

  /**
   * Handle not found error
   *
   * @param exc Exception to handle
   */
  @ExceptionHandler(AWENotFoundException.class)
  public ScreenData handleAWENotFoundException(AWException exc) {
    log.error("Error generating screen: Not found", exc);
    return screenService.getErrorScreenData("error-4xx");
  }
}
