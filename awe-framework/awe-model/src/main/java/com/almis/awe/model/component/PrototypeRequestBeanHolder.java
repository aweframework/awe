package com.almis.awe.model.component;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Component class to holder a PROTOTYPE scope bean with request info.
 * Used to share parameters request in distinct execution threads.
 */
public class PrototypeRequestBeanHolder {

  private static final ThreadLocal<RequestDataHolder> threadLocalBean = new ThreadLocal<>();

  public void setPrototypeBean(RequestDataHolder requestDataHolder) {
    threadLocalBean.set(requestDataHolder);
  }

  /**
   * Get prototype bean with request data
   * @return request data holder bean
   */
  public RequestDataHolder getPrototypeBean() {
    return threadLocalBean.get();
  }

  /**
   * Get request snapshot from current prototype holder.
   *
   * @return Deep copy of request data snapshot
   */
  public ObjectNode getRequestDataSnapshot() {
    RequestDataHolder requestDataHolder = getPrototypeBean();
    if (requestDataHolder == null || requestDataHolder.getRequestData() == null) {
      return null;
    }
    return requestDataHolder.getRequestData().deepCopy();
  }

  /**
   * Merge request data into current prototype holder.
   *
   * @param requestData request data to merge
   */
  public void mergeRequestData(ObjectNode requestData) {
    if (requestData == null) {
      return;
    }

    RequestDataHolder requestDataHolder = getPrototypeBean();
    if (requestDataHolder == null) {
      return;
    }

    ObjectNode currentRequestData = requestDataHolder.getRequestData();
    if (currentRequestData == null) {
      currentRequestData = JsonNodeFactory.instance.objectNode();
      requestDataHolder.setRequestData(currentRequestData);
    }

    currentRequestData.setAll(requestData.deepCopy());
  }

  /**
   * Remove prototype bean from thread local
   */
  public void clear() {
    threadLocalBean.remove();
  }
}