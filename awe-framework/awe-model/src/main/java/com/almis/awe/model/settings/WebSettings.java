package com.almis.awe.model.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * WebSettings component
 *
 * @author pgarcia
 */
@Data
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSettings {

  static final Integer MB = 1024 * 1024;
  // Settings
  private String pathServer;
  private String initialURL;
  private String language;
  private String theme;
  private String charset;
  private String applicationName;
  private String dataSuffix;
  private String homeScreen;
  private Integer recordsPerPage;
  private Integer recordsPerPageOnCriteria;
  private Integer pixelsPerCharacter;
  private String defaultComponentSize;
  private Boolean reloadCurrentScreen;
  private long suggestTimeout;
  // Connection
  private Integer connectionTimeout;
  private String cometUID;
  // Upload
  private String uploadIdentifier;
  private String downloadIdentifier;
  private long uploadMaxSize;
  private String addressIdentifier;
  // Security
  private String passwordPattern;
  private Integer minlengthPassword;
  private Boolean encodeTransmission;
  private String encodeKey;
  private String tokenKey;
  // Debug
  private String debug;
  // Loading timeout
  private long loadingTimeout;
  // Help timeout
  private long helpTimeout;
  // Message timeouts
  private WebTooltip messageTimeout;
  // Number options
  private WebNumberOptions numericOptions;
  // Pivot options
  private WebPivotOptions pivotOptions;
  // Chart options
  private WebChartOptions chartOptions;
}
