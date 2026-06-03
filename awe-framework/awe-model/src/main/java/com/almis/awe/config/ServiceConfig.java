package com.almis.awe.config;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Optional;

/**
 * Base class for all custom service
 *
 * @author Jorge BELLON
 */
public abstract class ServiceConfig implements ApplicationContextAware {

  // Injected services
  private ApplicationContext context;

  /**
   * Autowired application context
   *
   * @param context application context
   */
  @Autowired
  public void setApplicationContext(ApplicationContext context) {
    this.context = context;
  }

  /**
   * Get application context
   *
   * @return context
   */
  protected ApplicationContext getApplicationContext() {
    return context;
  }

  /**
   * Returns instantiated elements
   *
   * @return Awe Elements
   */
  public AweElements getElements() {
    return getBean(AweElements.class);
  }

  /**
   * Returns request object
   *
   * @return Awe request
   */
  public AweRequest getRequest() {
    try {
      AweRequest request = getBean(AweRequest.class);
      request.getParameterList();
      return request;
    } catch (Exception exc) {
      return null;
    }
  }

  /**
   * Retrieve query util helper.
   *
   * @return Query util bean
   */
  protected QueryUtil getQueryUtil() {
    return getBean(QueryUtil.class);
  }

  /**
   * Retrieve propagated request snapshot holder.
   *
   * @return Prototype request bean holder
   */
  protected PrototypeRequestBeanHolder getPrototypeRequestBeanHolder() {
    return getBean(PrototypeRequestBeanHolder.class);
  }

  /**
   * Retrieve request parameters using the async-safe fallback order implemented by QueryUtil.
   *
   * <p>This method is the preferred parameter access API for services extending {@link ServiceConfig},
   * especially when the code may run in asynchronous threads.</p>
   *
   * @return Request parameters or an empty object when no live request or propagated snapshot exists
   */
  public ObjectNode getRequestParameters() {
    return getQueryUtil().getParameters();
  }

  /**
   * Retrieve a mutable request parameter snapshot using the async-safe fallback order implemented by QueryUtil.
   *
   * <p>Use this helper when the current service needs to add or modify parameters for downstream operations.</p>
   *
   * @return Mutable request parameter snapshot
   */
  public ObjectNode getMutableRequestParameters() {
    return getRequestParameters();
  }

  /**
   * Retrieve a request parameter using the async-safe fallback order implemented by QueryUtil.
   *
   * @param name Parameter identifier
   * @return Parameter value or {@code null} when not present
   */
  public JsonNode getRequestParameter(String name) {
    return getQueryUtil().getRequestParameter(name);
  }

  /**
   * Retrieve a request parameter from a provided parameter snapshot.
   *
   * @param name       Parameter identifier
   * @param parameters Parameter snapshot
   * @return Parameter value or {@code null} when not present
   */
  public JsonNode getRequestParameter(String name, ObjectNode parameters) {
    return getQueryUtil().getRequestParameter(name, parameters);
  }

  /**
   * Retrieve a request parameter as string using the async-safe fallback order implemented by QueryUtil.
   *
   * @param name Parameter identifier
   * @return Parameter value as string or {@code null} when not present
   */
  public String getRequestParameterAsString(String name) {
    return Optional.ofNullable(getRequestParameter(name))
      .filter(parameter -> !parameter.isNull())
      .map(JsonNode::asText)
      .orElse(null);
  }

  /**
   * Retrieve a request parameter as string from a provided parameter snapshot.
   *
   * @param name       Parameter identifier
   * @param parameters Parameter snapshot
   * @return Parameter value as string or {@code null} when not present
   */
  public String getRequestParameterAsString(String name, ObjectNode parameters) {
    return Optional.ofNullable(getRequestParameter(name, parameters))
      .filter(parameter -> !parameter.isNull())
      .map(JsonNode::asText)
      .orElse(null);
  }

  /**
   * Write a request parameter into an explicit parameter snapshot.
   *
   * <p>Use this helper to make local parameter writes explicit in async-safe code paths.</p>
   *
   * @param parameters Parameter snapshot to mutate
   * @param name       Parameter identifier
   * @param value      Parameter value
   */
  public void putRequestParameter(ObjectNode parameters, String name, JsonNode value) {
    if (parameters == null || name == null) {
      return;
    }
    parameters.set(name, Optional.ofNullable(value).orElse(JsonNodeFactory.instance.nullNode()));
  }

  /**
   * Write a string request parameter into an explicit parameter snapshot.
   *
   * @param parameters Parameter snapshot to mutate
   * @param name       Parameter identifier
   * @param value      Parameter value
   */
  public void putRequestParameter(ObjectNode parameters, String name, String value) {
    putRequestParameter(parameters, name, value == null
      ? JsonNodeFactory.instance.nullNode()
      : JsonNodeFactory.instance.textNode(value));
  }

  /**
   * Write a request parameter into the propagated async snapshot of the current thread.
   *
   * <p>Use this helper only when descendant async hops must inherit a new parameter created or updated by the
   * current service.</p>
   *
   * @param name  Parameter identifier
   * @param value Parameter value
   */
  public void putPropagatedRequestParameter(String name, JsonNode value) {
    if (name == null) {
      return;
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    putRequestParameter(parameters, name, value);
    mergePropagatedRequestParameters(parameters);
  }

  /**
   * Write a string request parameter into the propagated async snapshot of the current thread.
   *
   * @param name  Parameter identifier
   * @param value Parameter value
   */
  public void putPropagatedRequestParameter(String name, String value) {
    if (name == null) {
      return;
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    putRequestParameter(parameters, name, value);
    mergePropagatedRequestParameters(parameters);
  }

  /**
   * Merge parameters into the propagated async snapshot of the current thread.
   *
   * <p>Use this helper only when descendant async hops must inherit parameters created or updated by the
   * current service.</p>
   *
   * @param parameters Parameter snapshot to merge into the propagated async snapshot
   */
  public void mergePropagatedRequestParameters(ObjectNode parameters) {
    getPrototypeRequestBeanHolder().mergeRequestData(parameters);
  }

  /**
   * Check if bean is defined
   *
   * @param bean Bean class
   * @return Bean
   */
  public boolean containsBean(String bean) {
    return context.containsBean(bean);
  }

  /**
   * Retrieve a bean
   *
   * @param clazz Bean class
   * @param <T>   Bean
   * @return Bean
   */
  public <T> T getBean(Class<T> clazz) {
    return context.getBean(clazz);
  }

  /**
   * Retrieve a bean
   *
   * @param beanId Bean identifier
   * @return Bean
   */
  public Object getBean(String beanId) {
    return context.getBean(beanId);
  }

  /**
   * Get current users session
   *
   * @return Awe Session
   */
  public AweSession getSession() {
    try {
      AweSession session = getBean(AweSession.class);
      session.getUser();
      return session;
    } catch (Exception exc) {
      return null;
    }
  }

  /**
   * Retrieve locale
   *
   * @param locale Locale identifier
   * @return Locale text
   */
  public String getLocale(String locale) {
    return getElements().getLocaleWithLanguage(locale, getElements().getLanguage());
  }

  /**
   * Retrieve locale with parameter
   *
   * @param locale     Locale identifier
   * @param parameters Parameter
   * @return Locale text
   */
  public String getLocale(String locale, String... parameters) {
    return getElements().getLocaleWithLanguage(locale, getElements().getLanguage(), (Object[]) parameters);
  }

  /**
   * Retrieve property
   *
   * @param property Property identifier
   * @return Property value
   */
  public String getProperty(String property) {
    return getElements().getProperty(property);
  }

  /**
   * Retrieve property of a determined class
   *
   * @param <T>      Return value class
   * @param property Property identifier
   * @param clazz    Property class
   * @return Property value
   */
  public <T> T getProperty(String property, Class<T> clazz) {
    return getElements().getProperty(property, clazz);
  }
}
