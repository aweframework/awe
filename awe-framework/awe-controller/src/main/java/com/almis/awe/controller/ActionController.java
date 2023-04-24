package com.almis.awe.controller;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.ActionService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manage all incoming action requests
 */
@RestController
@RequestMapping("/action")
public class ActionController {

  // Autowired services
  private final ActionService actionService;
  private final AweRequest request;

  /**
   * Autowired constructor
   *
   * @param actionService Action service
   * @param aweRequest    Awe request
   */
  @Autowired
  public ActionController(ActionService actionService, AweRequest aweRequest) {
    this.actionService = actionService;
    this.request = aweRequest;
  }

  /**
   * Launch server action
   *
   * @param actionId   Action identifier
   * @param parameters Parameters
   * @return Client action list
   */
  @PostMapping("/{actionId}")
  public List<ClientAction> launchAction(@PathVariable("actionId") String actionId,
                                         @RequestBody ObjectNode parameters) {

    // Initialize parameters
    request.setParameterList(parameters);

    // Span traces
   this.addCustomSpan("actionId", actionId);

    // Launch action
    return actionService.launchAction(actionId);
  }

  /**
   * Launch server action with target
   *
   * @param actionId   Action identifier
   * @param targetId   Target action
   * @param parameters Parameters
   * @return Client action list
   */
  @PostMapping("/{actionId}/{targetId}")
  public List<ClientAction> launchAction(@PathVariable("actionId") String actionId,
                                         @PathVariable("targetId") String targetId,
                                         @RequestBody ObjectNode parameters) {

    // Initialize parameters
    request.setTargetAction(targetId);
    request.setParameterList(parameters);

    // Span traces
    this.addCustomSpan("actionId", actionId);
    this.addCustomSpan("targetId", targetId);

    // Launch action
    return actionService.launchAction(actionId);
  }

  /**
   * Add custom span
   * @param key Span name
   * @param value Span value
   */
  private void addCustomSpan(String key, String value) {
    //Get current Span
    Span span = Span.current();
    //Add custom attributes to Span
    span.setAttribute(key, value);
  }
}
