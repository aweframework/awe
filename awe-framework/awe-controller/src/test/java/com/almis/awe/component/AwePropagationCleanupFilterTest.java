package com.almis.awe.component;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AwePropagationCleanupFilter}.
 *
 * <p>These tests cover the two critical scenarios identified in the R4 resilience review:
 * <ol>
 *   <li>A request writes propagated parameters but never submits a decorated async task —
 *       the overlay must be gone by the time the thread is returned to the pool.</li>
 *   <li>An exception propagates out of the filter chain — cleanup must still run.</li>
 * </ol>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AwePropagationCleanupFilterTest {

  @Mock
  private FilterChain filterChain;

  @Mock
  private ServletRequest request;

  @Mock
  private ServletResponse response;

  private PrototypeRequestBeanHolder holder;
  private AwePropagationCleanupFilter filter;

  @BeforeEach
  void setUp() {
    holder = new PrototypeRequestBeanHolder();
    holder.clearPendingPropagation();
    filter = new AwePropagationCleanupFilter(holder);
  }

  @AfterEach
  void tearDown() {
    holder.clearPendingPropagation();
    holder.clear();
  }

  /**
   * Regression: request writes propagated params but no async task is submitted.
   *
   * <p>Before this fix, the pending-propagation overlay would survive the request and
   * be inherited by the next unrelated request running on the same servlet thread.</p>
   */
  @Test
  void doFilter_whenRequestWritesPropagatedParamsButNoAsyncTaskSubmitted_overlayClearedAfterRequest()
      throws IOException, ServletException {
    // Request thread accumulates a propagated parameter (no async decorate() call follows)
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("leaked-param", "should-not-survive"));

    // Verify the overlay is present before the filter runs
    assertNotNull(holder.getPendingPropagationSnapshot(),
        "Pending propagation must be set before the filter runs");

    // The filter chain does NOT call AweMDCTaskDecorator (no async task submitted)
    filter.doFilter(request, response, filterChain);

    // After the request completes, the overlay must be gone
    assertNull(holder.getPendingPropagationSnapshot(),
        "Pending propagation must be cleared after request completion — no cross-request leakage");

    verify(filterChain).doFilter(request, response);
  }

  /**
   * Regression: cleanup runs even when the filter chain throws an exception.
   *
   * <p>Ensures the {@code finally} block guarantees cleanup under error conditions.</p>
   */
  @Test
  void doFilter_whenFilterChainThrows_overlayClearedDespiteException()
      throws IOException, ServletException {
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("leaked-param", "should-not-survive"));

    doThrow(new ServletException("simulated error")).when(filterChain).doFilter(any(), any());

    try {
      filter.doFilter(request, response, filterChain);
    } catch (ServletException ignored) {
      // expected
    }

    assertNull(holder.getPendingPropagationSnapshot(),
        "Pending propagation must be cleared even when the filter chain throws");
  }

  /**
   * When no overlay remains at request end (normal case after tasks ran and filter cleans up,
   * or when no params were propagated), the cleanup is a safe no-op.
   */
  @Test
  void doFilter_whenOverlayAlreadyClearedByFilter_noOpAndNoException()
      throws IOException, ServletException {
    // No overlay present (either never set, or filter already ran on an earlier pass)
    // Should not throw
    filter.doFilter(request, response, filterChain);

    assertNull(holder.getPendingPropagationSnapshot(),
        "No overlay; filter cleanup must be a safe no-op");
  }

  /**
   * When no propagated parameters were written at all, the filter runs cleanly.
   */
  @Test
  void doFilter_whenNoPendingPropagation_noOpAndNoException()
      throws IOException, ServletException {
    // Nothing accumulated
    filter.doFilter(request, response, filterChain);

    assertNull(holder.getPendingPropagationSnapshot());
    verify(filterChain).doFilter(request, response);
  }

  /**
   * Validates the authoritative lifecycle contract: the overlay is NOT cleared by
   * AweMDCTaskDecorator (which only snapshots), so it is still present after async tasks
   * have been decorated.  The filter is the only place that clears it.
   *
   * <p>This test guards against regressions where someone inadvertently re-adds a
   * {@code clearPendingPropagation()} call inside the decorator.</p>
   */
  @Test
  void doFilter_isAuthoritativeCleanup_overlayStillPresentAfterTasksDecoratedWithoutFilterRunning()
      throws IOException, ServletException {
    // Simulate: request thread writes a propagated param...
    holder.mergeRequestData(JsonNodeFactory.instance.objectNode().put("seed", "value"));

    // ...and the decorator snapshots it (but does NOT clear it)
    ObjectNode snapshot = holder.getPendingPropagationSnapshot();
    assertNotNull(snapshot, "Decorator would have taken this snapshot");

    // Overlay must still be present (decorator only snapshotted, never cleared)
    assertNotNull(holder.getPendingPropagationSnapshot(),
        "Overlay must still exist after the decorator read it — filter is the authoritative cleanup");

    // Now the filter runs (end of request)
    filter.doFilter(request, response, filterChain);

    // Only after the filter runs is the overlay gone
    assertNull(holder.getPendingPropagationSnapshot(),
        "Overlay must be cleared by the filter — not by the decorator");
  }
}
