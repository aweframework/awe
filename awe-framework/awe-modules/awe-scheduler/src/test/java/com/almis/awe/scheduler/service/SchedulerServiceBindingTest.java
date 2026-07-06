package com.almis.awe.scheduler.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the AWE service binding contract for the manual launch (Services.xml "executeTaskNow",
 * used by the Maintain.xml "LchTsk" target). The AWE JavaConnector resolves the target method by
 * EXACT parameter types (Class#getMethod), so the two-argument (Integer, String) signature MUST
 * remain present even after adding the three-argument variant that carries operator values.
 * Removing it silently breaks the existing manual launch at runtime.
 */
class SchedulerServiceBindingTest {

  /**
   * The two-argument signature bound by the AWE JavaConnector must exist on the local service.
   */
  @Test
  void schedulerServiceExposesLegacyExecuteTaskNowSignature() {
    assertDoesNotThrow(() -> assertNotNull(SchedulerService.class.getMethod("executeTaskNow", Integer.class, String.class)));
    // The three-argument variant used by the future operator-values modal must also exist.
    assertDoesNotThrow(() -> assertNotNull(SchedulerService.class.getMethod("executeTaskNow", Integer.class, String.class, Map.class)));
  }

  /**
   * The two-argument signature bound by the AWE JavaConnector must exist on the remote service.
   */
  @Test
  void remoteSchedulerServiceExposesLegacyExecuteTaskNowSignature() {
    assertDoesNotThrow(() -> assertNotNull(RemoteSchedulerService.class.getMethod("executeTaskNow", Integer.class, String.class)));
    // The three-argument variant used by the future operator-values modal must also exist.
    assertDoesNotThrow(() -> assertNotNull(RemoteSchedulerService.class.getMethod("executeTaskNow", Integer.class, String.class, Map.class)));
  }

  /**
   * The operator-values modal (Services.xml "executeTaskNowVariables") declares a bean-class +
   * list="true" service-parameter, which the AWE JavaConnector binds as List.class. The connector
   * resolves the target method by EXACT parameter types, so the (Integer, String, List) adapter
   * overload MUST exist on the class bound from Services.xml.
   */
  @Test
  void remoteSchedulerServiceExposesVariablesAdapterSignature() {
    assertDoesNotThrow(() -> assertNotNull(RemoteSchedulerService.class.getMethod("executeTaskNow", Integer.class, String.class, List.class)));
  }
}
