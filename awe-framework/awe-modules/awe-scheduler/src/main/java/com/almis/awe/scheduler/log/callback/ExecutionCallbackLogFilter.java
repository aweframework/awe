package com.almis.awe.scheduler.log.callback;

import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tags an admitted scheduler remote-maintain callback request with the propagated execution key,
 * so its traces are captured under the same execution as the scheduler-origin run (D3, D4).
 * <p>
 * Registered on {@code /api/maintain/*} only under the database execution-log store (see
 * {@code ExecutionLogStoreConfig}); never installed under the file-mode store. Runs after the
 * security filter chain, so {@link SecurityContextHolder} is already populated when admission is
 * evaluated (A3).
 * <p>
 * Admission is all-or-nothing (A4): the MDC is either set for the whole downstream chain or not
 * touched at all. A chain failure, or a failure inside admission itself, never fails the request
 * or leaks a partial MDC state (A5, A6, base ADR-8).
 */
@Slf4j
public class ExecutionCallbackLogFilter extends OncePerRequestFilter {

  private static final Pattern MAINTAIN_PATH = Pattern.compile("^/api/maintain/[^/]+$");
  private static final Pattern EXECUTION_KEY_SHAPE = Pattern.compile("^\\d{1,9}-\\d{1,9}$");
  private static final int MAX_HEADER_LENGTH = 32;

  private final ExecutionLogStore executionLogStore;
  private final boolean secureEnabled;
  private final String callbackUser;

  /**
   * @param executionLogStore Active execution log store, completed for the application origin
   * @param remoteEnabled     Whether this deployment uses a remote scheduler; the startup notices
   *                          below only concern callback traffic, which an embedded scheduler
   *                          never produces
   * @param secureEnabled     Whether the callback requires the configured callback user (A3);
   *                          when {@code false}, a well-formed header is honored unconditionally,
   *                          trusting network isolation as the boundary (A3', logged once here)
   * @param callbackUser      Configured remote-callback user name, checked under {@code secureEnabled}
   */
  public ExecutionCallbackLogFilter(ExecutionLogStore executionLogStore, boolean remoteEnabled,
                                    boolean secureEnabled, String callbackUser) {
    this.executionLogStore = executionLogStore;
    this.secureEnabled = secureEnabled;
    this.callbackUser = callbackUser;
    if (!remoteEnabled) {
      return;
    }
    if (!secureEnabled) {
      log.warn("[SCHEDULER][EXECUTION_LOG] remote-callback-secure-enabled=false: the {} callback "
          + "header is honored with no principal check. The deployment network (pod-to-pod, never "
          + "exposed through an ingress) is the trust boundary for this endpoint.",
        TaskConstants.EXECUTION_KEY_HEADER);
    } else if (callbackUser == null || callbackUser.isBlank()) {
      log.warn("[SCHEDULER][EXECUTION_LOG] remote-callback-secure-enabled=true but "
          + "awe.scheduler.remote-callback-user is not set on this application: no request can ever "
          + "satisfy the callback principal check, so callback capture stays off until that property "
          + "is configured to match the scheduler's own remote-callback-user.");
    }
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ExecutionKey key;
    try {
      key = admittedKey(request);
    } catch (RuntimeException exc) {
      log.debug("[SCHEDULER][EXECUTION_LOG] Callback admission check failed; proceeding untagged", exc);
      key = null;
    }

    if (key == null) {
      filterChain.doFilter(request, response);
      return;
    }

    MDC.put(TaskConstants.LOG_BY_TASK_EXECUTION, key.mdcKey());
    MDC.put(TaskConstants.EXECUTION_LOG_ORIGIN, ExecutionLogOrigin.APPLICATION.code());
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(TaskConstants.LOG_BY_TASK_EXECUTION);
      MDC.remove(TaskConstants.EXECUTION_LOG_ORIGIN);
      completeQuietly(key);
    }
  }

  private void completeQuietly(ExecutionKey key) {
    try {
      executionLogStore.complete(key, ExecutionLogOrigin.APPLICATION);
    } catch (RuntimeException exc) {
      log.debug("[SCHEDULER][EXECUTION_LOG] Could not signal application-origin completion", exc);
    }
  }

  private ExecutionKey admittedKey(HttpServletRequest request) {
    if (!MAINTAIN_PATH.matcher(request.getServletPath()).matches()) {
      return null;
    }

    ExecutionKey key = executionKeyFromHeader(request);
    if (key == null) {
      return null;
    }

    if (secureEnabled && !isAuthenticatedAsCallbackUser()) {
      return null;
    }

    return key;
  }

  private ExecutionKey executionKeyFromHeader(HttpServletRequest request) {
    List<String> values = Collections.list(request.getHeaders(TaskConstants.EXECUTION_KEY_HEADER));
    if (values.size() != 1) {
      return null;
    }

    String value = values.get(0);
    if (value == null || value.length() > MAX_HEADER_LENGTH || !EXECUTION_KEY_SHAPE.matcher(value).matches()) {
      return null;
    }

    return ExecutionKey.of(value);
  }

  private boolean isAuthenticatedAsCallbackUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
      && authentication.isAuthenticated()
      && !(authentication instanceof AnonymousAuthenticationToken)
      && callbackUser != null
      && callbackUser.equals(authentication.getName());
  }
}
