package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The query of a deferred execution reads its trace
 */
class SchedulerDeferredScreensTest {

  private static final String BASE = "application/awe-scheduler/";

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
