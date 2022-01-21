package com.almis.awe.component;

import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Tasl decorator to propagate MDC parameters to child threads
 */
public class AweMDCTaskDecorator implements TaskDecorator {

    @NotNull
    @Override
    public Runnable decorate(@NotNull Runnable runnable) {
      Map<String, String> contextMap = MDC.getCopyOfContextMap();
      return () -> {
        try {
          MDC.setContextMap(Optional.ofNullable(contextMap).orElse(Collections.emptyMap()));
          runnable.run();
        } finally {
          MDC.clear();
        }
      };
    }
}
