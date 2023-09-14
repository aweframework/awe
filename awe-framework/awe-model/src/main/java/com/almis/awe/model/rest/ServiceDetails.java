package com.almis.awe.model.rest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Rest service details
 * @author Pablo Vidal
 * @since 2.5
 */
@Data
@Accessors(chain = true)
@Validated
public class ServiceDetails {
  /**
   * Service name
   */
  private String name;
  /**
   * Service connector base url
   */
  @NotNull
  @URL
  private String baseUrl;
  /**
   * Service parameter list
   */
  private List<RestParameter> parameters;
  /**
   * Service authentication
   */
  private ServiceAuth authentication;
}
