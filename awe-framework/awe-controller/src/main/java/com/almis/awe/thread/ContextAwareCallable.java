package com.almis.awe.thread;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Callable;

/**
 * Created by pgarcia on 07/04/2017.
 * @param <T> type of ContextAwareCallable
 */
public class ContextAwareCallable<T> implements Callable<T> {
  private final Callable<T> task;
  private final RequestAttributes context;
  private final SecurityContext securityContext;

  /**
   * Constructor
   *
   * @param task task
   * @param context context
   */
  public ContextAwareCallable(Callable<T> task, RequestAttributes context, SecurityContext securityContext) {
    this.task = task;
    this.context = context;
    this.securityContext = securityContext;
  }

  /**
   * Call function
   *
   * @return launch ContextAwareCallable
   * @throws Exception exception
   */
  @Override
  public T call() throws Exception {
    if (context != null) {
      RequestContextHolder.setRequestAttributes(context);
    }
    if (securityContext != null) {
      SecurityContextHolder.setContext(securityContext);
    }

    try {
      return task.call();
    } finally {
      // Clear context
      RequestContextHolder.resetRequestAttributes();
      SecurityContextHolder.clearContext();
    }
  }
}
