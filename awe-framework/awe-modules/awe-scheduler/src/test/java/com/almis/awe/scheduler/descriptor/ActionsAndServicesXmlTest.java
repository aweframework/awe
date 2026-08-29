package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural (golden) checks on the {@code get-execution-log} action and its {@code getExecutionLog}
 * service: the database-mode resolving action (ADR-5), mirroring the generic {@code get-log-file}
 * action's {@code log-delta} response shape without touching it.
 */
class ActionsAndServicesXmlTest {

  @Test
  void getExecutionLogActionRespondsWithLogDeltaAndTheSharedLogContentVariable() throws IOException {
    String xml = readResource("application/awe-scheduler/global/Actions.xml");

    int start = xml.indexOf("<action id=\"get-execution-log\"");
    assertTrue(start >= 0, "get-execution-log action must exist");
    String action = xml.substring(start, xml.indexOf("</action>", start));

    assertTrue(action.contains("<call service=\"getExecutionLog\" />"));
    assertTrue(action.contains("<type>log-delta</type>"));
    assertTrue(action.contains("<parameter name=\"log\" variable=\"LOG_CONTENT\" />"));
    assertTrue(action.contains("<parameter name=\"replace\" variable=\"LOG_CONTENT_REPLACE\" />"));
    assertTrue(action.contains("<parameter name=\"version\" variable=\"LOG_CONTENT_VERSION\" />"));
  }

  @Test
  void getExecutionLogServiceIsBoundToSchedulerService() throws IOException {
    String service = getExecutionLogService();

    assertTrue(service.contains("classname=\"com.almis.awe.scheduler.service.SchedulerService\" method=\"getExecutionLog\""));
  }

  @Test
  void getExecutionLogServiceDeclaresNoServiceParameters() throws IOException {
    String service = getExecutionLogService();

    assertFalse(service.contains("<service-parameter"));
  }

  private String getExecutionLogService() throws IOException {
    String xml = readResource("application/awe-scheduler/global/Services.xml");

    int start = xml.indexOf("<service id=\"getExecutionLog\">");
    assertTrue(start >= 0, "getExecutionLog service must exist");
    return xml.substring(start, xml.indexOf("</service>", start));
  }

  private String readResource(String resource) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertTrue(stream != null, resource + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
