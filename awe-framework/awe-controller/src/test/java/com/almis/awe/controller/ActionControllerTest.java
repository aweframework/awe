package com.almis.awe.controller;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.ActionService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionControllerTest {

  @Mock
  private ActionService actionService;

  @Mock
  private AweRequest aweRequest;

  @InjectMocks
  private ActionController actionController;

  @Test
  void testLaunchAction() {
    // Setup
    String actionId = "testAction";
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();

    List<ClientAction> expected = new ArrayList<>();
    when(actionService.launchAction(actionId)).thenReturn(expected);

    // Call
    List<ClientAction> result = actionController.launchAction(actionId, parameters);

    // Verify
    assertEquals(expected, result);
  }

  @Test
  void testLaunchActionWithTarget() {
    // Setup
    String actionId = "testAction";
    String targetId = "testTarget";
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();

    List<ClientAction> expected = new ArrayList<>();
    when(actionService.launchAction(actionId)).thenReturn(expected);

    // Call
    List<ClientAction> result = actionController.launchAction(actionId, targetId, parameters);

    // Verify
    assertEquals(expected, result);
  }
}