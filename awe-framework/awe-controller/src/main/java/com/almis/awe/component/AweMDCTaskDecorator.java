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

  @NotNull
  @Override
  public Runnable decorate(@NotNull Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    // Retrieve awe request from context
    RequestDataHolder requestDataHolder = requestDataHolderProvider.getObject();
    ObjectNode requestParameters = Optional.ofNullable(RequestContextHolder.getRequestAttributes()).map(requestAttr -> (AweRequest) requestAttr.getAttribute("scopedTarget.aweRequest", RequestAttributes.SCOPE_REQUEST)).map(AweRequest::getParametersSafe).orElse(JsonNodeFactory.instance.objectNode());
    requestDataHolder.setRequestData(requestParameters);

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
}
