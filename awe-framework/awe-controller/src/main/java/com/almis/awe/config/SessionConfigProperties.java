package com.almis.awe.config;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  private Map<String, String> parameter = Stream.of(initMap()).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  @NotNull
  private String[][] initMap() {
    return new String[][]{
            {"module", "ModNamByOpeSel"},
            {"site", "SitNamByOpeSel"},
            {"database", "DbsAlsBySitModSel"},
    };
  }
}
