package com.almis.awe.model.component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Holder for a prototype-scoped request data bean shared across async threads.
 *
 * <p>Maintains two independent ThreadLocals:</p>
 * <ul>
 *   <li>{@code threadLocalBean} — propagated snapshot set by
 *       {@code com.almis.awe.component.AweMDCTaskDecorator} before each async task runs</li>
 *   <li>{@code pendingPropagation} — per-thread overlay written by
 *       {@link com.almis.awe.config.ServiceConfig#putPropagatedRequestParameter} /
 *       {@link com.almis.awe.config.ServiceConfig#mergePropagatedRequestParameters}.
 *       Read as an immutable deep copy by the decorator, never cleared there.
 *       The overlay is removed by {@code com.almis.awe.component.AwePropagationCleanupFilter}
 *       (servlet threads) or by the decorator's {@code finally} block (worker threads).</li>
 * </ul>
 */
public class PrototypeRequestBeanHolder {

  private static final ThreadLocal<RequestDataHolder> threadLocalBean = new ThreadLocal<>();
  private static final ThreadLocal<ObjectNode> pendingPropagation = new ThreadLocal<>();

  public void setPrototypeBean(RequestDataHolder requestDataHolder) {
    threadLocalBean.set(requestDataHolder);
  }

  /**
   * Get the propagated snapshot for the current thread.
   * @return request data holder bean, or {@code null} if none set
   */
  public RequestDataHolder getPrototypeBean() {
    return threadLocalBean.get();
  }

  /**
   * Deep copy of the current thread's request data snapshot.
   * @return deep copy, or {@code null} if no snapshot is available
   */
  public ObjectNode getRequestDataSnapshot() {
    RequestDataHolder holder = getPrototypeBean();
    if (holder == null || holder.getRequestData() == null) {
      return null;
    }
    return holder.getRequestData().deepCopy();
  }

  /**
   * Merge request data into the current thread's parameter store.
   *
   * <p>If inside a decorated async task (holder active), data is merged directly into the holder.
   * Otherwise (request thread), data accumulates in the {@code pendingPropagation} overlay so
   * {@code com.almis.awe.component.AweMDCTaskDecorator} includes it in every async snapshot.</p>
   *
   * @param requestData data to merge
   */
  public void mergeRequestData(ObjectNode requestData) {
    if (requestData == null) {
      return;
    }

    RequestDataHolder holder = getPrototypeBean();
    if (holder == null) {
      // Request thread: accumulate in pending overlay for first async hop
      ObjectNode pending = pendingPropagation.get();
      if (pending == null) {
        pending = JsonNodeFactory.instance.objectNode();
        pendingPropagation.set(pending);
      }
      pending.setAll(requestData.deepCopy());
      return;
    }

    ObjectNode current = holder.getRequestData();
    if (current == null) {
      current = JsonNodeFactory.instance.objectNode();
      holder.setRequestData(current);
    }
    current.setAll(requestData.deepCopy());
  }

  /**
   * Deep copy of the pending-propagation overlay for the current thread.
   * The overlay is NOT removed — sibling tasks within the same request each get a consistent copy.
   *
   * @return deep copy, or {@code null} if no overlay exists
   */
  public ObjectNode getPendingPropagationSnapshot() {
    ObjectNode pending = pendingPropagation.get();
    return pending != null ? pending.deepCopy() : null;
  }

  /**
   * Remove the pending-propagation overlay from the current thread.
   *
 * <p>Called by {@code com.almis.awe.component.AwePropagationCleanupFilter} (servlet threads)
 * and by {@code com.almis.awe.component.AweMDCTaskDecorator}'s finally block (worker threads).
   * Safe no-op when nothing was written ({@link ThreadLocal#remove()} on absent value).</p>
   */
  public void clearPendingPropagation() {
    pendingPropagation.remove();
  }

  /** Remove the propagated snapshot from the current thread. */
  public void clear() {
    threadLocalBean.remove();
  }
}
