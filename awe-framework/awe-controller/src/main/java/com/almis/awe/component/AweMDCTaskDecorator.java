package com.almis.awe.component;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Task decorator that propagates MDC context and request parameters to async worker threads.
 *
 * <p>Every {@link #decorate} call builds an immutable parameter snapshot from three sources
 * (increasing precedence):</p>
 * <ol>
 *   <li><strong>Live request</strong> — current {@code AweRequest} parameters (base)</li>
 *   <li><strong>Ancestor snapshot</strong> — {@link RequestDataHolder} from the parent hop;
 *       prevents propagated values from being overwritten by the re-installed live request
 *       on child → grandchild hops</li>
 *   <li><strong>Pending overlay</strong> — parameters written on the current thread via
 *       {@link com.almis.awe.config.ServiceConfig#putPropagatedRequestParameter} /
 *       {@link com.almis.awe.config.ServiceConfig#mergePropagatedRequestParameters} (always wins)</li>
 * </ol>
 *
 * <p><strong>Cleanup boundaries:</strong> the {@code pendingPropagation} ThreadLocal is never cleared
 * by the decorator — it is read as an immutable deep copy only.  The servlet thread overlay is
 * cleared by {@link AwePropagationCleanupFilter} at request end; the worker-thread overlay is
 * cleared in this task's {@code finally} block.  See {@link PrototypeRequestBeanHolder}.</p>
 */
@Slf4j
@Component
public class AweMDCTaskDecorator implements TaskDecorator {

  private static final String SCOPED_AWE_REQUEST_ATTRIBUTE = "scopedTarget.aweRequest";

  private final ObjectProvider<RequestDataHolder> requestDataHolderProvider;
  private final PrototypeRequestBeanHolder prototypeRequestBeanHolder;

  public AweMDCTaskDecorator(ObjectProvider<RequestDataHolder> requestDataHolderProvider,
                              PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    this.requestDataHolderProvider = requestDataHolderProvider;
    this.prototypeRequestBeanHolder = prototypeRequestBeanHolder;
  }

  @NotNull
  @Override
  public Runnable decorate(@NotNull Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    RequestDataHolder requestDataHolder = requestDataHolderProvider.getObject();
    requestDataHolder.setRequestData(resolveRequestSnapshot());

    return () -> {
      MDC.setContextMap(Optional.ofNullable(contextMap).orElse(Collections.emptyMap()));
      prototypeRequestBeanHolder.setPrototypeBean(requestDataHolder);
      try {
        runnable.run();
      } finally {
        MDC.clear();
        prototypeRequestBeanHolder.clear();
        // Authoritative worker-thread cleanup: any overlay written during this task is removed
        // before the thread returns to the pool.  Safe no-op when nothing was written.
        prototypeRequestBeanHolder.clearPendingPropagation();
        log.trace("[ASYNC] Cleaned up propagation state on worker thread '{}'",
            Thread.currentThread().getName());
      }
    };
  }

  /**
   * Build the parameter snapshot for the async task by merging three sources in increasing
   * precedence order: live request → ancestor snapshot → pending overlay.
   */
  private ObjectNode resolveRequestSnapshot() {
    // Read the pending overlay as an immutable deep copy — never clear it here;
    // AwePropagationCleanupFilter (servlet threads) or this decorator's finally block
    // (worker threads) handle cleanup.
    ObjectNode pendingOverlay = prototypeRequestBeanHolder.getPendingPropagationSnapshot();

    ObjectNode liveRequestSnapshot = resolveLiveRequestSnapshot();
    ObjectNode ancestorSnapshot = prototypeRequestBeanHolder.getRequestDataSnapshot();

    // Layer 1: live request (lowest precedence)
    ObjectNode result = liveRequestSnapshot != null
        ? liveRequestSnapshot.deepCopy()
        : JsonNodeFactory.instance.objectNode();

    // Layer 2: ancestor snapshot — keeps propagated values across hops
    if (ancestorSnapshot != null) {
      result.setAll(ancestorSnapshot);
    }

    // Layer 3: pending overlay (highest precedence)
    if (pendingOverlay != null && !pendingOverlay.isEmpty()) {
      result.setAll(pendingOverlay);
    }

    return result;
  }

  private ObjectNode resolveLiveRequestSnapshot() {
    try {
      return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(attrs -> attrs.getAttribute(SCOPED_AWE_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
        .filter(AweRequest.class::isInstance)
        .map(AweRequest.class::cast)
        .map(AweRequest::getParametersSafe)
        .orElse(null);
    } catch (RuntimeException exc) {
      log.warn("[ASYNC] Failed to resolve live request snapshot on thread '{}': {}",
          Thread.currentThread().getName(), exc.getMessage());
      return null;
    }
  }
}
