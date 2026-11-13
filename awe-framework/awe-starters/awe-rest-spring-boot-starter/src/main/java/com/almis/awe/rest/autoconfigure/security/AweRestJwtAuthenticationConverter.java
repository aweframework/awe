package com.almis.awe.rest.autoconfigure.security;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.security.authentication.ClaimRolesAuthoritiesConverter;
import com.almis.awe.service.user.AweUserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Convert validated JWT claims into a synthetic AWE principal. The roles of the profile claim are mapped to AWE
 * profiles as the single sign-on login maps them (authority prefix, profile names without case, profile priority),
 * and each profile gets one role and the permissions the local login gives to it, read on every conversion.
 */
@Slf4j
public class AweRestJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final AweRestConfigProperties restConfigProperties;
  private final Supplier<AweUserDetailService> userDetailService;

  /**
   * Converter constructor.
   *
   * @param baseConfigProperties Base application config
   * @param securityConfigProperties Security config
   * @param restConfigProperties Rest config
   * @param userDetailService User detail service that maps the roles and reads the permissions of the profiles
   */
  public AweRestJwtAuthenticationConverter(BaseConfigProperties baseConfigProperties,
                                           SecurityConfigProperties securityConfigProperties,
                                           AweRestConfigProperties restConfigProperties,
                                           Supplier<AweUserDetailService> userDetailService) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.restConfigProperties = restConfigProperties;
    this.userDetailService = userDetailService;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    AweRestConfigProperties.Principal principalConfig = restConfigProperties.getOauth2ResourceServer().getPrincipal();

    Identity identity = resolveIdentity(jwt, principalConfig);

    // The first profile is the main one, the rest are additional profiles: one role per profile, then the permissions
    // of the AWE profiles
    AweUserDetailService service = userDetailService.get();
    String defaultProfile = StringUtils.hasText(principalConfig.getDefaultProfile())
      ? principalConfig.getDefaultProfile() : baseConfigProperties.getDefaultRole();
    List<UserProfile> aweProfiles = mapProfiles(jwt, principalConfig, service);
    if (aweProfiles.isEmpty()) {
      aweProfiles = findDefaultProfile(service, defaultProfile);
    }
    // A default profile that is not an AWE profile keeps its role, without permissions
    List<UserProfile> profiles = aweProfiles.isEmpty()
      ? List.of(new UserProfile(defaultProfile, defaultProfile, null)) : aweProfiles;
    UserProfile profile = profiles.getFirst();

    Collection<GrantedAuthority> authorities = Stream.concat(
        profiles.stream().<GrantedAuthority>map(aweProfile -> new SimpleGrantedAuthority("ROLE_" + aweProfile.name())),
        service.getPermissionAuthorities(aweProfiles.stream().map(UserProfile::acronym).toList()).stream())
      .toList();

    AweUserDetails userDetails = new AweUserDetails()
      .setUsername(identity.username())
      .setName(identity.name())
      .setEmail(identity.email())
      .setProfile(profile.acronym())
      .setProfileName(profile.name())
      .setRestrictions(StringUtils.hasText(profile.restriction()) ? profile.restriction()
        : securityConfigProperties.getDefaultRestriction())
      .setLanguage(baseConfigProperties.getLanguageDefault())
      .setTheme(baseConfigProperties.getTheme())
      .setInitialScreen(baseConfigProperties.getScreen() != null ? baseConfigProperties.getScreen().getInitial() : null)
      .setEnabled(true)
      .setAccountNonExpired(true)
      .setAccountNonLocked(true)
      .setCredentialsNonExpired(true)
      .setEnabled2fa(false)
      .setFullyAuthenticated(true)
      .setAdditionalProfiles(profiles.stream().skip(1).toList())
      .setAuthorities(authorities);

    return new UsernamePasswordAuthenticationToken(userDetails, jwt, authorities);
  }

  /**
   * Resolve the user of the token: the delegated user when a delegated username claim is present, otherwise the
   * client (client id or subject, with the client principal prefix). The display name falls back to the username
   *
   * @param jwt             Token
   * @param principalConfig Principal configuration
   * @return Username, display name and email (null for a client)
   */
  private Identity resolveIdentity(Jwt jwt, AweRestConfigProperties.Principal principalConfig) {
    String delegatedUsername = firstNonBlankClaim(jwt, principalConfig.getDelegatedUsernameClaims());
    if (StringUtils.hasText(delegatedUsername)) {
      String name = firstNonBlankClaim(jwt, principalConfig.getDelegatedDisplayNameClaims());
      return new Identity(delegatedUsername, StringUtils.hasText(name) ? name : delegatedUsername,
        firstNonBlankClaim(jwt, principalConfig.getDelegatedEmailClaims()));
    }

    String clientId = firstNonBlankClaim(jwt, principalConfig.getClientIdClaims());
    String identity = StringUtils.hasText(clientId) ? clientId : asString(jwt.getClaim("sub"));
    if (!StringUtils.hasText(identity)) {
      throw new OAuth2AuthenticationException(
        new OAuth2Error("invalid_token", "Token identity claims are missing", null)
      );
    }
    String prefix = principalConfig.getClientPrincipalPrefix();
    String username = StringUtils.hasText(prefix) ? prefix + identity : identity;
    String name = firstNonBlankClaim(jwt, principalConfig.getClientDisplayNameClaims());
    return new Identity(username, StringUtils.hasText(name) ? name : username, null);
  }

  /**
   * AWE profile of the default profile, empty when it is not an AWE profile
   *
   * @param service        User detail service
   * @param defaultProfile Default profile name
   * @return The default profile, or empty
   */
  private List<UserProfile> findDefaultProfile(AweUserDetailService service, String defaultProfile) {
    try {
      return service.findProfile(defaultProfile).map(List::of).orElse(List.of());
    } catch (AWException exc) {
      throw new AuthenticationServiceException("The default profile could not be retrieved", exc);
    }
  }

  /**
   * AWE profiles of the roles of the profile claim, mapped as the single sign-on login maps them. The claim name may be
   * a dotted path to a nested claim, and it may hold a list of roles or a single one. When the claim is configured, no
   * role maps to an AWE profile and a role is required ({@code awe.security.sso.require-role}), the token is refused
   *
   * @param jwt              Token
   * @param principalConfig  Principal configuration
   * @param service          User detail service
   * @return AWE profiles, main profile first (empty when the claim is not configured or no role maps)
   */
  private List<UserProfile> mapProfiles(Jwt jwt, AweRestConfigProperties.Principal principalConfig,
                                        AweUserDetailService service) {
    String profileClaim = principalConfig.getProfileClaim();
    if (!StringUtils.hasText(profileClaim)) {
      return List.of();
    }
    List<UserProfile> profiles;
    try {
      Collection<GrantedAuthority> roles = new ClaimRolesAuthoritiesConverter(List.of(profileClaim))
        .convert(jwt.getClaims());
      profiles = service.mapProfiles(roles);
    } catch (AWException exc) {
      throw new AuthenticationServiceException("The profiles of the token could not be retrieved", exc);
    }
    if (profiles.isEmpty() && securityConfigProperties.getSso().isRoleRequired()) {
      log.warn("REST token refused: no role of the profile claim maps to an AWE profile");
      throw new OAuth2AuthenticationException(
        new OAuth2Error("invalid_token", "No role of the token maps to an AWE profile", null));
    }
    return profiles;
  }

  private String firstNonBlankClaim(Jwt jwt, List<String> claimNames) {
    for (String claimName : claimNames) {
      String claimValue = asString(jwt.getClaim(claimName));
      if (StringUtils.hasText(claimValue)) {
        return claimValue;
      }
    }
    return null;
  }

  private String asString(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String stringValue) {
      return stringValue;
    }

    if (value instanceof Collection<?> collectionValue) {
      return collectionValue.stream()
        .map(this::asString)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
    }

    return String.valueOf(value);
  }

  /**
   * User of the token
   *
   * @param username Username
   * @param name     Display name
   * @param email    Email, null when the token has none
   */
  private record Identity(String username, String name, String email) {
  }
}
