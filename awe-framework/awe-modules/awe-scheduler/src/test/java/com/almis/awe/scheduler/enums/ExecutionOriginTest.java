package com.almis.awe.scheduler.enums;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.almis.awe.scheduler.constant.TaskConstants.WEBHOOK_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Origin of the executions in the activity screen (#993)
 */
class ExecutionOriginTest {

  @Test
  void anExecutionLaunchedByAWebhookHasTheWebhookOrigin() {
    assertEquals(Optional.of("webhook"), ExecutionOrigin.fromGroup(WEBHOOK_GROUP).map(ExecutionOrigin::getCode));
    assertEquals(Optional.of(WEBHOOK_GROUP), ExecutionOrigin.fromCode("webhook").map(ExecutionOrigin::getGroup));
  }
}
