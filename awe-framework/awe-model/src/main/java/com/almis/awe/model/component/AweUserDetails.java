package com.almis.awe.model.component;

import com.almis.awe.model.dto.UserProfile;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

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
  /**
   * Marks a FORCE-mode user who has not yet enrolled a TOTP secret.
   * While true, the user may only access the 2FA enrollment flow;
   * general private application access is denied via {@code AweSession.isAuthenticated()}.
   * Cleared by {@code TotpService.generate2faSecret()} once enrollment completes.
   */
  private boolean pendingTotpEnrollment;
  /**
   * Marks that a fresh TOTP secret was just generated in this session and has not yet
   * been verified by the user. While true, {@code /access/qr-code} is permitted so the
   * user can scan the QR. Cleared when {@code AweSessionDetails.onLoginSuccess()} is called
   * after successful TOTP verification.
   */
  private boolean freshEnrollment;
  private String profile;
  private String profileName;
  private String restrictions;
  /**
   * Restriction file of the user itself (empty when the user takes the restriction file of its main profile)
   */
  private String userRestriction;
  private String theme;
  private String language;
  private String initialScreen;
  private boolean fullyAuthenticated;
  private List<UserProfile> additionalProfiles = new ArrayList<>();

  public AweUserDetails() {
    this.authorities = new ArrayList<>();
    this.accountNonExpired = true;
    this.accountNonLocked = true;
    this.credentialsNonExpired = true;
    this.enabled = true;
    this.timeBeforeExpiration = 2147483647;
    this.graceLoginsRemaining = 2147483647;
  }

  /**
   * Retrieve the acronyms of every profile of the user: the main profile first, then the additional profiles
   *
   * @return Profile acronyms, without repetitions
   */
  public List<String> getProfiles() {
    return Stream.concat(Stream.of(profile), additionalProfiles.stream().map(UserProfile::acronym))
      .filter(StringUtils::isNotBlank)
      .distinct()
      .toList();
  }

  /**
   * Retrieve the restriction files of every profile of the user: the restriction of the main profile (or of the user)
   * first, then the restriction files of the additional profiles that have one
   *
   * @return Restriction files, without repetitions
   */
  public List<String> getProfileRestrictions() {
    return Stream.concat(Stream.of(restrictions), additionalProfiles.stream().map(UserProfile::restriction))
      .filter(StringUtils::isNotBlank)
      .distinct()
      .toList();
  }

  public void addAuthority(GrantedAuthority a) {
    if (!this.hasAuthority(a)) {
      this.authorities.add(a);
    }
  }

  private boolean hasAuthority(GrantedAuthority a) {
    return this.authorities.stream().anyMatch(grantedAuthority -> grantedAuthority.equals(a));
  }

  // ---------------------------------------------------------------------------
  // Named auth-state predicates — use these instead of raw boolean combinations
  // ---------------------------------------------------------------------------

  /**
   * Whether this user is in the FORCE-mode pre-enrollment window.
   * <p>
   * When true the user has authenticated with a password but has not yet generated
   * a TOTP secret. They may only access the enrollment flow.
   *
   * @return true if the user must complete 2FA enrollment before any private access
   */
  public boolean isAwaitingTotpEnrollment() {
    return pendingTotpEnrollment;
  }

  /**
   * Whether this enrolled user has completed TOTP verification in this session.
   * <p>
   * An enrolled user ({@code enabled2fa=true}) who has not yet passed TOTP
   * verification is considered partially authenticated.
   *
   * @return true only when 2FA is enrolled AND the current session passed TOTP verification
   */
  public boolean hasTotpVerified() {
    return enabled2fa && fullyAuthenticated;
  }

  /**
   * Whether this user is allowed to fetch the QR code image.
   * <p>
   * Permitted in exactly two cases:
   * <ul>
   *   <li>Fully authenticated (settings flow — user is already in a private session).</li>
   *   <li>Fresh enrollment window: a secret was just generated in this session and
   *       has not yet been scanned/verified ({@code freshEnrollment=true}).</li>
   * </ul>
   * Denied in all other cases, including pending enrollment before any secret is
   * generated (which would also result in a null secret runtime error).
   *
   * @return true when the current session state allows fetching the QR PNG
   */
  public boolean mayFetchQrCode() {
    return fullyAuthenticated || freshEnrollment;
  }
}
