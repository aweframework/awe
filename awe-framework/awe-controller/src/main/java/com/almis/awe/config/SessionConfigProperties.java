package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Awe session configuration properties
 */
@ConfigurationProperties(prefix = "awe.session")
@Data
@Validated
public class SessionConfigProperties {
  /**
   * Parameters map injected to session when the application context is starting.
   * Ex: [ParameterName, QueryName]
   */
  private Map<String, String> parameter = new LinkedHashMap<>();

  /**
   * SessionConfigProperties constructor
   */
  public SessionConfigProperties() {
    this.parameter.put("module", "ModNamByOpeSel");
    this.parameter.put("site", "SitNamByOpeSel");
    this.parameter.put("database", "DbsAlsBySitModSel");
    this.parameter.put("themeMode", "getUserThemeMode");
  }
}