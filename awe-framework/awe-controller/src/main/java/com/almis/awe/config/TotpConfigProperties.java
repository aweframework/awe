package com.almis.awe.config;

import com.almis.awe.model.type.SecondFactorStatusType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "awe.security.totp")
public class TotpConfigProperties {
  /**
   * Disable, enable or force the TOTP
   * Default value disabled
   */
  private SecondFactorStatusType enabled = SecondFactorStatusType.DISABLED;
  /**
   * Initial screen to check TOTP
   * Default value check-2fa
   */
  private String initialScreen = "check-2fa";
  /**
   * Initial screen to force enable TOTP
   * Default value activate-2fa
   */
  private String activateScreen = "activate-2fa";
}
