package com.almis.awe.rest.autoconfigure.security;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * Convert validated JWT claims into a synthetic AWE principal.
 */
public class AweRestJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final AweRestConfigProperties restConfigProperties;

  /**
   * Converter constructor.
   *
   * @param baseConfigProperties Base application config
   * @param securityConfigProperties Security config
   * @param restConfigProperties Rest config
   */
  public AweRestJwtAuthenticationConverter(BaseConfigProperties baseConfigProperties,
                                           SecurityConfigProperties securityConfigProperties,
                                           AweRestConfigProperties restConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.restConfigProperties = restConfigProperties;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    AweRestConfigProperties.Principal principalConfig = restConfigProperties.getOauth2ResourceServer().getPrincipal();

    String delegatedUsername = firstNonBlankClaim(jwt, principalConfig.getDelegatedUsernameClaims());
    String clientId = firstNonBlankClaim(jwt, principalConfig.getClientIdClaims());
    String subject = asString(jwt.getClaim("sub"));

    String username;
    String name;
    String email;

    if (StringUtils.hasText(delegatedUsername)) {
      username = delegatedUsername;
      name = firstNonBlankClaim(jwt, principalConfig.getDelegatedDisplayNameClaims());
      if (!StringUtils.hasText(name)) {
        name = username;
      }
      email = firstNonBlankClaim(jwt, principalConfig.getDelegatedEmailClaims());
    } else {
      String identity = StringUtils.hasText(clientId) ? clientId : subject;
      if (!StringUtils.hasText(identity)) {
        throw new OAuth2AuthenticationException(
          new OAuth2Error("invalid_token", "Token identity claims are missing", null)
        );
      }
      String prefix = principalConfig.getClientPrincipalPrefix();
      username = StringUtils.hasText(prefix) ? prefix + identity : identity;
      name = firstNonBlankClaim(jwt, principalConfig.getClientDisplayNameClaims());
      if (!StringUtils.hasText(name)) {
        name = username;
      }
      email = null;
    }

    String profile = resolveProfile(jwt, principalConfig);

    Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + profile));

    AweUserDetails userDetails = new AweUserDetails()
      .setUsername(username)
      .setName(name)
      .setEmail(email)
      .setProfile(profile)
      .setProfileName(profile)
      .setRestrictions(securityConfigProperties.getDefaultRestriction())
      .setLanguage(baseConfigProperties.getLanguageDefault())
      .setTheme(baseConfigProperties.getTheme())
      .setInitialScreen(baseConfigProperties.getScreen() != null ? baseConfigProperties.getScreen().getInitial() : null)
      .setEnabled(true)
      .setAccountNonExpired(true)
      .setAccountNonLocked(true)
      .setCredentialsNonExpired(true)
      .setEnabled2fa(false)
      .setFullyAuthenticated(true)
      .setAuthorities(authorities);

    return new UsernamePasswordAuthenticationToken(userDetails, jwt, authorities);
  }

  private String resolveProfile(Jwt jwt, AweRestConfigProperties.Principal principalConfig) {
    String configuredProfileClaim = principalConfig.getProfileClaim();
    if (StringUtils.hasText(configuredProfileClaim)) {
      String profileFromToken = asString(jwt.getClaim(configuredProfileClaim));
      if (StringUtils.hasText(profileFromToken)) {
        return profileFromToken;
      }
    }

    if (StringUtils.hasText(principalConfig.getDefaultProfile())) {
      return principalConfig.getDefaultProfile();
    }

    return baseConfigProperties.getDefaultRole();
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
}
