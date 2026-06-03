package com.almis.awe.component;

import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.component.RequestDataHolder;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Task decorator to propagate MDC parameters to child threads
 */
@Component
public class AweMDCTaskDecorator implements TaskDecorator {

  // Autowired services
  private final ObjectProvider<RequestDataHolder> requestDataHolderProvider;
  private final PrototypeRequestBeanHolder prototypeRequestBeanHolder;

  /**
   * AWEMDCTaskDecorator constructor
   *
   * @param requestDataHolderProvider  request data holder provider
   * @param prototypeRequestBeanHolder prototype request data bean holder
   */
  public AweMDCTaskDecorator(ObjectProvider<RequestDataHolder> requestDataHolderProvider, PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    this.requestDataHolderProvider = requestDataHolderProvider;
    this.prototypeRequestBeanHolder = prototypeRequestBeanHolder;
  }

  private static final String SCOPED_AWE_REQUEST_ATTRIBUTE = "scopedTarget.aweRequest";

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
        // Clear context
        MDC.clear();
        prototypeRequestBeanHolder.clear();
      }
    };
  }

  private ObjectNode resolveRequestSnapshot() {
    ObjectNode liveRequestSnapshot = resolveLiveRequestSnapshot();
    if (liveRequestSnapshot != null) {
      return liveRequestSnapshot;
    }

    ObjectNode ancestorSnapshot = prototypeRequestBeanHolder.getRequestDataSnapshot();
    if (ancestorSnapshot != null) {
      return ancestorSnapshot;
    }

    return JsonNodeFactory.instance.objectNode();
  }

  private ObjectNode resolveLiveRequestSnapshot() {
    try {
      return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
        .map(requestAttributes -> requestAttributes.getAttribute(SCOPED_AWE_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST))
        .filter(AweRequest.class::isInstance)
        .map(AweRequest.class::cast)
        .map(AweRequest::getParametersSafe)
        .orElse(null);
    } catch (RuntimeException exc) {
      return null;
    }
  }
}
