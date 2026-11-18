package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The scheduler screens show the deferred executions: the counters of the dashboard and the trace in the execution
 * details
 */
class SchedulerDeferredScreensTest {

  private static final String BASE = "application/awe-scheduler/";

  @Test
  void theDashboardCountsTheDeferredExecutionsByOutcome() throws IOException {
    String dashboard = read("screen/scheduler-dashboard.xml");
    String queries = read("global/Queries.xml");

    for (String counter : new String[]{"SchDfrRun", "SchDfrApi", "SchDfrDln", "SchDfrUsr"}) {
      assertTrue(dashboard.contains("initial-load=\"query\" target-action=\"" + counter + "\""), counter);
      assertTrue(queries.contains("<query id=\"" + counter + "\" permission=\"scheduler.read\">"), counter);
    }
    assertTrue(query(queries, "SchDfrRun").contains("<table query=\"getRunningDeferredExecutions\""));
    assertTrue(query(queries, "SchDfrApi").contains("value=\"API\""));
    assertTrue(query(queries, "SchDfrDln").contains("value=\"DEADLINE\""));
    assertTrue(query(queries, "SchDfrUsr").contains("value=\"USER\""));
  }

  @Test
  void theExecutionDetailsShowTheDeferredTraceOnlyWhenThereIsOne() throws IOException {
    String screen = read("screen/scheduler-execution-data.xml");
    int start = screen.indexOf("<criteria id=\"execution-deferred\"");
    assertTrue(start >= 0, "execution-deferred criterion");
    String criterion = screen.substring(start, screen.indexOf("</criteria>", start));
    assertTrue(criterion.contains("<dependency-element id=\"execution-deferred\" condition=\"is not empty\"/>"));
  }

  @Test
  void theDeferredQueryReadsTheTrace() throws IOException {
    String deferred = query(read("global/Queries.xml"), "getDeferredExecution");
    for (String alias : new String[]{"completionDate", "completionSource", "cancellationStatus", "cancellationError"}) {
      assertTrue(deferred.contains("alias=\"" + alias + "\""), alias);
    }
  }

  private String query(String xml, String id) {
    int start = xml.indexOf("<query id=\"" + id + "\"");
    assertTrue(start >= 0, id);
    return xml.substring(start, xml.indexOf("</query>", start));
  }

  private String read(String resource) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BASE + resource)) {
      assertNotNull(stream, resource);
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
