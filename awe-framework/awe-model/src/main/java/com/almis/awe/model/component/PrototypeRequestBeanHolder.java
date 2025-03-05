package com.almis.awe.model.component;

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
   * Remove prototype bean from thread local
   */
  public void clear() {
    threadLocalBean.remove();
  }
}