package com.almis.awe.model.dto;

import com.almis.awe.model.type.EmailAuthenticationType;
import com.almis.awe.model.type.EmailSecurityMode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Optional;

@Data
@Accessors(chain = true)
public class EmailServer {
  private String name;
  private String host;
  private Integer port;
  private EmailSecurityMode securityMode;

  // Legacy flag: server needs user and password
  private boolean authenticated;
  // Authentication code (AweEmlSrv.Ath): 0 none, 1 user and password, 2 OAuth2
  private Integer authentication;
  private String user;
  @ToString.Exclude
  private String pass;
  // Spring Security client registration (client_credentials) of OAuth2 servers (AweEmlSrv.OauReg)
  private String oauthRegistration;

  /**
   * Retrieve the authentication type. Without authentication code, the legacy authenticated flag decides.
   *
   * @return Authentication type
   */
  @JsonIgnore
  public EmailAuthenticationType getAuthenticationType() {
    if (authentication != null) {
      return EmailAuthenticationType.fromCode(authentication);
    }
    return authenticated ? EmailAuthenticationType.BASIC : EmailAuthenticationType.NONE;
  }

  /**
   * Retrieve the security mode, NONE when not defined
   *
   * @return Security mode
   */
  @JsonIgnore
  public EmailSecurityMode getSecurityModeOrDefault() {
    return Optional.ofNullable(securityMode).orElse(EmailSecurityMode.NONE);
  }

  /**
   * Retrieve the port, the security mode default port when not defined
   *
   * @return Port
   */
  @JsonIgnore
  public int getPortOrDefault() {
    return Optional.ofNullable(port).orElseGet(() -> getSecurityModeOrDefault().getDefaultPort());
  }
}
