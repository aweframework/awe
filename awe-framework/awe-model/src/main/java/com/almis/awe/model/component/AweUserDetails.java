package com.almis.awe.model.component;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Accessors(chain = true)
public class AweUserDetails implements UserDetails {

  private String name;
  private String dn;
  private Collection<GrantedAuthority> authorities;
  @ToString.Exclude
  private String password;
  private String username;
  private String email;
  private boolean accountNonExpired;
  private boolean accountNonLocked;
  private boolean credentialsNonExpired;
  private boolean enabled;
  private Integer timeBeforeExpiration;
  private Integer graceLoginsRemaining;
  private boolean enabled2fa;
  @ToString.Exclude
  private String secret2fa;
  private String profile;
  private String restrictions;
  private String theme;
  private String language;
  private String initialScreen;
  private boolean fullyAuthenticated;

  public AweUserDetails() {
    this.authorities = new ArrayList<>();
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
    this.enabled = true;
    this.timeBeforeExpiration = 2147483647;
    this.graceLoginsRemaining = 2147483647;
  }

  public void addAuthority(GrantedAuthority a) {
    if (!this.hasAuthority(a)) {
      this.authorities.add(a);
    }
  }

  private boolean hasAuthority(GrantedAuthority a) {
    return this.authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.equals(a));
  }
}
