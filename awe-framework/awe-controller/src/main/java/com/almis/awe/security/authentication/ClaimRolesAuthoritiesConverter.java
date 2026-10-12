package com.almis.awe.security.authentication;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Converts the roles carried by the claims of an identity provider token into granted authorities.
 *
 * <p>The claims are read in the configured order ({@code awe.security.sso.roles-claims}), for example
 * {@code realm_access.roles} (Keycloak realm roles), {@code resource_access.my-client.roles} (Keycloak client roles) or
 * {@code roles} (Microsoft Entra ID app roles). A claim name that exists as it is in the token is used directly, so
 * URI claim names with dots work; otherwise the dots walk nested objects. A claim may hold a list of roles or a single
 * text role. Roles are kept in the order found, without duplicates.</p>
 *
 * <p>Only claim names and role counts are logged, never role values.</p>
 */
@Slf4j
public class ClaimRolesAuthoritiesConverter implements Converter<Map<String, Object>, Collection<GrantedAuthority>> {

  private static final String PATH_SEPARATOR = ".";

  private final List<String> claimNames;

  /**
   * Create the converter
   *
   * @param claimNames names (or dotted paths) of the claims that carry the roles
   */
  public ClaimRolesAuthoritiesConverter(List<String> claimNames) {
    this.claimNames = claimNames == null ? List.of() : claimNames.stream()
      .filter(StringUtils::isNotBlank)
      .map(String::trim)
      .toList();
  }

  @Override
  public Collection<GrantedAuthority> convert(@NonNull Map<String, Object> claims) {
    Set<String> roles = new LinkedHashSet<>();
    for (String claimName : claimNames) {
      List<String> claimRoles = roles(claimValue(claims, claimName));
      log.debug("Roles claim {} carries {} role(s)", claimName, claimRoles.size());
      roles.addAll(claimRoles);
    }
    return roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role)).toList();
  }

  private static Object claimValue(Map<String, Object> claims, String claimName) {
    if (claims.containsKey(claimName)) {
      return claims.get(claimName);
    }
    Object value = claims;
    for (String part : StringUtils.split(claimName, PATH_SEPARATOR)) {
      if (!(value instanceof Map<?, ?> map)) {
        return null;
      }
      value = map.get(part);
    }
    return value;
  }

  private static List<String> roles(Object value) {
    Stream<?> values;
    if (value instanceof Collection<?> collection) {
      values = collection.stream();
    } else if (value instanceof String text) {
      values = Stream.of(text);
    } else {
      values = Stream.empty();
    }
    return values
      .filter(Objects::nonNull)
      .map(Object::toString)
      .filter(StringUtils::isNotBlank)
      .toList();
  }
}
