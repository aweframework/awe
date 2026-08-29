package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogLine;
import com.almis.awe.scheduler.log.ExecutionLogPage;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.Date;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of {@code ExecutionCallbackLogFilter} (D3, D4) registered for real on a
 * live embedded server: a real JWT-authenticated {@code POST /api/maintain/{target}} request
 * flows through the actual Spring Security chain and the actual
 * {@code FilterRegistrationBean}-registered callback filter, proving the V1 filter-ordering
 * assumption ({@link org.springframework.security.core.context.SecurityContextHolder} is
 * populated when admission runs) without a mocked {@code SecurityContext}.
 * <p>
 * Runs on a random port (not the module's shared fixed-port context) so its
 * {@code database}-mode {@code @TestPropertySource} never collides with the default-config
 * fixed-port context other {@code AbstractSpringFixedEnvironmentIT} tests share.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
  "awe.scheduler.execution-log-store=database",
  "awe.scheduler.remote-callback-secure-enabled=true",
  "awe.scheduler.remote-callback-user=" + SchedulerExecutionCallbackLogTest.CALLBACK_USER,
  "awe.scheduler.execution-log-flush-interval=100ms",
  "awe.scheduler.execution-log-batch-size=5"
})
class SchedulerExecutionCallbackLogTest {

  static final String CALLBACK_USER = "test";
  private static final String OTHER_AUTHENTICATED_USER = "donald";
  private static final String TRACE_TARGET = "ExecutionLogCallbackTrace";
  private static final String APPLICATION_ORIGIN_MARKER = "Launching a test service";
  private static final long AWAIT_TIMEOUT_MILLIS = 10_000L;
  private static final long AWAIT_POLL_MILLIS = 50L;

  @LocalServerPort
  private int port;

  @Autowired
  private JWTTokenService jwtTokenService;

  @Autowired
  private ExecutionLogStore executionLogStore;

  @Autowired
  private DataSource dataSource;

  private final TestRestTemplate restTemplate = new TestRestTemplate();

  @Test
  void secureCallbackFromTheConfiguredCallbackUserIsAdmittedAndCapturesApplicationOriginRows() {
    ExecutionKey key = new ExecutionKey(9501, 1);

    ResponseEntity<AweRestResponse> response = postMaintain(CALLBACK_USER, key.mdcKey());

    assertEquals(HttpStatus.OK, response.getStatusCode());
    awaitCondition(() -> countOriginRows(key, "A") > 0,
      "Admitted callback must eventually persist at least one application-origin (Src='A') row");
  }

  @Test
  void secureCallbackFromADifferentAuthenticatedPrincipalIsRejectedWithoutApplicationOriginRows() throws Exception {
    ExecutionKey key = new ExecutionKey(9501, 2);

    ResponseEntity<AweRestResponse> response = postMaintain(OTHER_AUTHENTICATED_USER, key.mdcKey());

    assertEquals(HttpStatus.OK, response.getStatusCode(), "The maintain itself must still succeed, untagged");
    Thread.sleep(300);
    assertEquals(0L, countOriginRows(key, "A"), "A non-callback-user principal must never be tagged");
  }

  @Test
  void malformedHeaderIsRejectedWithoutRowsAndTheRequestStillSucceeds() throws Exception {
    ExecutionKey key = new ExecutionKey(9501, 3);

    ResponseEntity<AweRestResponse> response = postMaintain(CALLBACK_USER, "not-a-valid-execution-key");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Thread.sleep(300);
    assertEquals(0L, countOriginRows(key, "A"), "A malformed header must never be tagged");
  }

  @Test
  void twoOriginReadMergesTheSchedulerAndApplicationWindowsIntoOneTimeOrderedStream() throws Exception {
    ExecutionKey key = new ExecutionKey(9502, 1);

    // Scheduler-origin line, tagged exactly as JobService.startLogging would on the job thread.
    executionLogStore.append(new ExecutionLogLine(key, ExecutionLogOrigin.SCHEDULER, System.currentTimeMillis(), "scheduler-origin-line"));
    executionLogStore.complete(key, ExecutionLogOrigin.SCHEDULER);

    // Application-origin line, captured through the real registered callback filter over HTTP
    // (D4's single-context two-origin proof: both origins tagged in this one JVM).
    ResponseEntity<AweRestResponse> response = postMaintain(CALLBACK_USER, key.mdcKey());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // offset 0 is always append mode: an empty caller has nothing to replace.
    ExecutionLogPage page = awaitPage(key, mergedPage ->
      mergedPage.lines().stream().anyMatch(line -> line.contains(APPLICATION_ORIGIN_MARKER)));

    assertFalse(page.replace(), "offset 0 must always be append mode, never replace");
    assertTrue(page.lines().stream().anyMatch(line -> line.contains("scheduler-origin-line")),
      "Merged window must include the scheduler-origin line: " + page.lines());
    assertTrue(page.lines().stream().anyMatch(line -> line.contains(APPLICATION_ORIGIN_MARKER)),
      "Merged window must include the application-origin line: " + page.lines());
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }

  private HttpHeaders headersFor(String username) {
    String jwtToken = JWT.create()
      .withSubject(username)
      .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    return headers;
  }

  private ResponseEntity<AweRestResponse> postMaintain(String username, String executionKeyHeaderValue) {
    HttpHeaders headers = headersFor(username);
    if (executionKeyHeaderValue != null) {
      headers.set(TaskConstants.EXECUTION_KEY_HEADER, executionKeyHeaderValue);
    }
    HttpEntity<String> entity = new HttpEntity<>(null, headers);
    return restTemplate.exchange(createURLWithPort("/api/maintain/" + TRACE_TARGET), HttpMethod.POST, entity, AweRestResponse.class);
  }

  private long countOriginRows(ExecutionKey key, String originCode) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    Long count = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM AweSchExeLog WHERE IdeTsk = ? AND ExeTsk = ? AND Src = ?",
      Long.class, key.taskId(), key.executionId(), originCode);
    return count == null ? 0L : count;
  }

  private void awaitCondition(java.util.function.BooleanSupplier condition, String failureMessage) {
    long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MILLIS;
    while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
      sleepQuietly();
    }
    assertTrue(condition.getAsBoolean(), failureMessage);
  }

  private ExecutionLogPage awaitPage(ExecutionKey key, Predicate<ExecutionLogPage> satisfied) throws Exception {
    long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MILLIS;
    ExecutionLogPage page = executionLogStore.read(key, 0, null);
    while (!satisfied.test(page) && System.currentTimeMillis() < deadline) {
      Thread.sleep(AWAIT_POLL_MILLIS);
      page = executionLogStore.read(key, 0, null);
    }
    return page;
  }

  private void sleepQuietly() {
    try {
      Thread.sleep(AWAIT_POLL_MILLIS);
    } catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
    }
  }
}
