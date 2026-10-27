package com.almis.awe.model.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Branding of the sign-in page sent to the client (awe.application.login.*). Empty values are not sent
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebLoginSettings {
  private String layout;
  private String logo;
  private String logoDark;
  private String background;
  private String backgroundEffect;
  private String title;
  private String subtitle;
  private String panelTitle;
  private String panelSubtitle;
  private String footerText;
  private Boolean poweredBy;
  private Boolean languageSelector;
  private Boolean modeSelector;
  private List<String> languages;
}
