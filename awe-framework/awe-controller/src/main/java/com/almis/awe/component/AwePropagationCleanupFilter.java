package com.almis.awe.component;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Servlet filter that clears the {@code pendingPropagation} ThreadLocal overlay at the end
 * of every request.
 *
 * <p>This is the <strong>sole cleanup point</strong> for the overlay on servlet threads.
 * {@link AweMDCTaskDecorator} reads the overlay as an immutable deep copy per {@code decorate()}
 * call but never clears it, so sibling async tasks within the same request all see a consistent
 * snapshot. Only this filter removes it, in a {@code finally} block, preventing stale values from
 * leaking onto the next request on the same servlet thread.</p>
 *
 * <p>Calling {@link PrototypeRequestBeanHolder#clearPendingPropagation()} when nothing was
 * written is a safe no-op ({@link ThreadLocal#remove()} on an absent value is harmless).</p>
 */
@Slf4j
public class AwePropagationCleanupFilter implements Filter {

  private final PrototypeRequestBeanHolder prototypeRequestBeanHolder;

  public AwePropagationCleanupFilter(PrototypeRequestBeanHolder prototypeRequestBeanHolder) {
    this.prototypeRequestBeanHolder = prototypeRequestBeanHolder;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // no initialisation needed
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      chain.doFilter(request, response);
    } finally {
      prototypeRequestBeanHolder.clearPendingPropagation();
      if (log.isTraceEnabled() && request instanceof HttpServletRequest) {
        log.trace("[FILTER] Cleared pending propagation overlay after request '{}'",
            ((HttpServletRequest) request).getRequestURI());
      }
    }
  }

  @Override
  public void destroy() {
    // no teardown needed
  }
}
