package com.almis.awe.model.rest;

import com.almis.awe.model.type.ServiceAuthType;
import lombok.Data;

@Data
public class ServiceAuth {

  /**
   * Service authentication type
   */
  private ServiceAuthType type = ServiceAuthType.BASIC;
  /**
   * Username value for basic authentication
   */
  private String username;
  /**
   * Password value for basic authentication
   */
  private String password;
}
