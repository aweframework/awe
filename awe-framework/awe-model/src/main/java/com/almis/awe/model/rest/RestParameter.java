package com.almis.awe.model.rest;

import lombok.Data;


/**
 * Rest parameter class
 */
@Data
public class RestParameter {
  /**
   * Parameter name of service
   */
  private String name;
  /**
   * Parameter value of service
   */
  private String value;
  /**
   * Parameter type of service.
   * Default value
   */
  private ServiceParameterType type = ServiceParameterType.VALUE;

  /**
   * ServiceParameter type Enum
   */
  public enum ServiceParameterType {
    /**
     * Parameter from session
     */
    SESSION,
    /**
     * Parameter from request
     */
    REQUEST,
    /**
     * Parameter from variable
     */
    VARIABLE,
    /**
     * Parameter with static value
     */
    VALUE
  }
}
