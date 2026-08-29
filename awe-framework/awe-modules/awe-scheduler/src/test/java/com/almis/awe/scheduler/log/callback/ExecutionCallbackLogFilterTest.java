package com.almis.awe.scheduler.log.callback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Threat-matrix table (design.md §6, T1-T8): every case is a trusted-callback admission rule
 * (A1-A6) exercised against a naive implementation that would otherwise tag or leak state on an
 * untrusted, malformed, or failing request.
 * <p>
 * Coverage note: {@link org.springframework.web.filter.OncePerRequestFilter} skips {@code ERROR}
 * dispatches, so container-level error-page rendering falls outside the tagged MDC window, while
 * an {@code @ExceptionHandler} response (still on the original {@code REQUEST} dispatch) stays
 * inside it.
 */
@ExtendWith(MockitoExtension.class)
class ExecutionCallbackLogFilterTest {

  private static final String CALLBACK_USER = "scheduler-callback";
  private static final ExecutionKey EXECUTION_KEY = new ExecutionKey(12, 34);
  private static final String MAINTAIN_PATH = "/api/maintain/CstNew";

  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain filterChain;

  @AfterEach
  void clearSecurityContextAndMdc() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  // ---- A1 / T4: only the exact single-segment maintain path is a callback candidate ----

  @ParameterizedTest
  @ValueSource(strings = {"/api/maintain/async/CstNew", "/api/maintain/public/CstNew", "/api/maintain/a/b", "/api/other/CstNew"})
  void requestsOutsideTheSingleSegmentMaintainPathAreNeverTagged(String path) throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    when(request.getServletPath()).thenReturn(path);
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
  }

  /**
   * A non-root {@code server.servlet.context-path} makes {@link HttpServletRequest#getRequestURI()}
   * include the context path prefix (e.g. {@code /scheduler-app/api/maintain/CstNew}), which never
   * matches {@code ^/api/maintain/[^/]+$}. {@link HttpServletRequest#getServletPath()} already
   * excludes the context path, so admission must match against it, not {@code getRequestURI()}.
   */
  @Test
  void aNonRootContextPathStillAdmitsTheSingleSegmentMaintainPath() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    when(request.getServletPath()).thenReturn(MAINTAIN_PATH);
    when(request.getHeaders(TaskConstants.EXECUTION_KEY_HEADER))
      .thenReturn(Collections.enumeration(List.of("12-34")));

    filter.doFilterInternal(request, response, filterChain);

    verify(executionLogStore).complete(eq(EXECUTION_KEY), eq(ExecutionLogOrigin.APPLICATION));
    verify(request, never()).getRequestURI();
  }

  @Test
  void theExactSingleSegmentMaintainPathIsAdmitted() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");

    filter.doFilterInternal(request, response, filterChain);

    verify(executionLogStore).complete(eq(EXECUTION_KEY), eq(ExecutionLogOrigin.APPLICATION));
  }

  // ---- A2 / T3: malformed execution-key shapes are rejected, never logged raw ----

  @ParameterizedTest
  @ValueSource(strings = {"abc-1", "1-", "1-2-3", "", "123456789-1234567890123456789012", "12_34", "12.34"})
  void malformedExecutionKeysAreNeverTagged(String malformedValue) throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, malformedValue);
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
  }

  @Test
  void aMultiValuedHeaderIsNeverTagged() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    when(request.getServletPath()).thenReturn(MAINTAIN_PATH);
    when(request.getHeaders(TaskConstants.EXECUTION_KEY_HEADER))
      .thenReturn(Collections.enumeration(List.of("12-34", "56-78")));
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
  }

  @Test
  void theRawMalformedHeaderValueNeverAppearsInAnyLogLine() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    String sensitiveValue = "TOTALLY-INVALID-MARKER-9999";
    mockRequest(MAINTAIN_PATH, sensitiveValue);

    Logger logger = (Logger) LoggerFactory.getLogger(ExecutionCallbackLogFilter.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      filter.doFilterInternal(request, response, filterChain);
    } finally {
      detachListAppender(logAppender);
      logger.setLevel(originalLevel);
    }

    assertEquals(0, logAppender.list.stream().filter(event -> event.getFormattedMessage().contains(sensitiveValue)).count());
  }

  // ---- A3 / T1 / T2: secure-mode principal admission ----

  @Test
  void secureModeAdmitsOnlyTheConfiguredCallbackUser() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    setAuthenticatedPrincipal(CALLBACK_USER);

    filter.doFilterInternal(request, response, filterChain);

    verify(executionLogStore).complete(eq(EXECUTION_KEY), eq(ExecutionLogOrigin.APPLICATION));
  }

  @Test
  void secureModeRejectsADifferentAuthenticatedPrincipal() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    setAuthenticatedPrincipal("someone-else");
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
  }

  @Test
  void secureModeRejectsAnUnauthenticatedAnonymousCaller() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    SecurityContextHolder.getContext().setAuthentication(
      new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
  }

  @Test
  void secureModeRejectsWhenNoAuthenticationIsPresentAtAll() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    assertUntaggedDuringChain();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
  }

  // ---- A3' / T5: insecure mode honors the header, network isolation is the trust boundary ----

  @Test
  void insecureModeHonorsAWellFormedHeaderFromAnyCaller() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");

    filter.doFilterInternal(request, response, filterChain);

    verify(executionLogStore).complete(eq(EXECUTION_KEY), eq(ExecutionLogOrigin.APPLICATION));
  }

  @Test
  void insecureModeLogsExactlyOneStartupWarningNamingTheNetworkIsolationTrustBoundary() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, true, false, CALLBACK_USER);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(1, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  @Test
  void embeddedSchedulerLogsNoStartupWarningEvenWithoutACallbackUser() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, false, true, null);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(0, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  @Test
  void embeddedSchedulerLogsNoStartupWarningWhenCallbackSecurityIsDisabled() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, false, false, null);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(0, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  @Test
  void secureModeLogsNoStartupWarning() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, true, true, CALLBACK_USER);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(0, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  // ---- A3'' / W3: secure mode with no usable callback user can never admit any request ----

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void secureModeWithABlankOrMissingCallbackUserLogsExactlyOneStartupWarning(String blankUser) {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, true, true, blankUser);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(1, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  @Test
  void secureModeWithANullCallbackUserLogsExactlyOneStartupWarning() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, true, true, null);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(1, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  @Test
  void insecureModeWithANullCallbackUserLogsOnlyTheNetworkIsolationWarning() {
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      new ExecutionCallbackLogFilter(executionLogStore, true, false, null);
    } finally {
      detachListAppender(logAppender);
    }

    assertEquals(1, logAppender.list.stream().filter(event -> event.getLevel() == Level.WARN).count());
  }

  /**
   * Spec scenario "Failed remote maintain captures its stack trace": the MDC discriminator and
   * origin are visible for the entire chain duration, so any logging the maintain body performs
   * while the chain runs - including a thrown exception's stack trace - is captured under the
   * propagated execution key before the filter's {@code finally} clears it.
   */
  @Test
  void theMdcCarriesTheExecutionKeyAndApplicationOriginForTheEntireChainDuration() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    doAnswer(invocation -> {
      assertEquals("12-34", MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
      assertEquals(ExecutionLogOrigin.APPLICATION.code(), MDC.get(TaskConstants.EXECUTION_LOG_ORIGIN));
      return null;
    }).when(filterChain).doFilter(request, response);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  // ---- A5 / T6: a chain failure after admission still clears MDC and completes exactly once ----

  @Test
  void aChainExceptionAfterAdmissionStillClearsMdcAndCompletesExactlyOnce() throws Exception {
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    doThrow(new IllegalStateException("boom")).when(filterChain).doFilter(request, response);

    assertThrows(IllegalStateException.class, () -> filter.doFilterInternal(request, response, filterChain));

    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
    assertNull(MDC.get(TaskConstants.EXECUTION_LOG_ORIGIN));
    verify(executionLogStore, times(1)).complete(eq(EXECUTION_KEY), eq(ExecutionLogOrigin.APPLICATION));
  }

  // ---- A6 / T7: admission logic itself throws (poisoned SecurityContext) ----

  @Test
  void aPoisonedSecurityContextIsCaughtAndTheRequestProceedsUntagged() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    Authentication poisoned = mock(Authentication.class);
    when(poisoned.isAuthenticated()).thenThrow(new IllegalStateException("poisoned"));
    SecurityContextHolder.getContext().setAuthentication(poisoned);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verifyNoInteractions(executionLogStore);
    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
  }

  @Test
  void thePoisonedSecurityContextFailureIsLoggedExactlyOnceAtDebug() throws Exception {
    ExecutionCallbackLogFilter filter = secureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");
    Authentication poisoned = mock(Authentication.class);
    when(poisoned.isAuthenticated()).thenThrow(new IllegalStateException("poisoned"));
    SecurityContextHolder.getContext().setAuthentication(poisoned);

    Logger logger = (Logger) LoggerFactory.getLogger(ExecutionCallbackLogFilter.class);
    Level originalLevel = logger.getLevel();
    logger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      filter.doFilterInternal(request, response, filterChain);
    } finally {
      detachListAppender(logAppender);
      logger.setLevel(originalLevel);
    }

    assertEquals(1, logAppender.list.stream().filter(event -> event.getLevel() == Level.DEBUG).count());
  }

  // ---- T8 (re-asserted at the filter boundary): a failing store never fails the request ----

  @Test
  void aFailingStoreCompletionSignalNeverPropagatesFromTheFilter() throws Exception {
    doThrow(new IllegalStateException("store down")).when(executionLogStore).complete(any(), any());
    ExecutionCallbackLogFilter filter = insecureFilter();
    mockRequest(MAINTAIN_PATH, "12-34");

    assertDoesNotThrow(() -> filter.doFilterInternal(request, response, filterChain));

    verify(filterChain).doFilter(request, response);
  }

  // ---- helpers ----

  private ExecutionCallbackLogFilter insecureFilter() {
    return new ExecutionCallbackLogFilter(executionLogStore, true, false, CALLBACK_USER);
  }

  private ExecutionCallbackLogFilter secureFilter() {
    return new ExecutionCallbackLogFilter(executionLogStore, true, true, CALLBACK_USER);
  }

  private void mockRequest(String path, String executionKeyHeaderValue) {
    when(request.getServletPath()).thenReturn(path);
    when(request.getHeaders(TaskConstants.EXECUTION_KEY_HEADER))
      .thenReturn(Collections.enumeration(List.of(executionKeyHeaderValue)));
  }

  /**
   * Stubs the mocked chain to assert, mid-request (before {@code doFilterInternal} returns and
   * runs its {@code finally}), that no MDC tag was ever set. A post-{@code finally} assertion alone
   * cannot distinguish "never tagged" from "tagged, then correctly cleaned up" (gate corrective).
   */
  private void assertUntaggedDuringChain() throws Exception {
    doAnswer(invocation -> {
      assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
      assertNull(MDC.get(TaskConstants.EXECUTION_LOG_ORIGIN));
      return null;
    }).when(filterChain).doFilter(request, response);
  }

  private void setAuthenticatedPrincipal(String principalName) {
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken(principalName, "N/A", List.of()));
  }

  private ListAppender<ILoggingEvent> attachListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(ExecutionCallbackLogFilter.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
    return logAppender;
  }

  private void detachListAppender(ListAppender<ILoggingEvent> logAppender) {
    Logger logger = (Logger) LoggerFactory.getLogger(ExecutionCallbackLogFilter.class);
    logger.detachAppender(logAppender);
  }
}
