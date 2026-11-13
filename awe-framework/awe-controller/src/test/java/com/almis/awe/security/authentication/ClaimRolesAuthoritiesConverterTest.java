package com.almis.awe.security.authentication;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the provider roles from configurable claims (Keycloak realm or client roles, Entra ID app roles...)
 */
@DisplayName("Roles from configurable claims")
class ClaimRolesAuthoritiesConverterTest {

  private static final String REALM_ROLES = "realm_access.roles";
  private static final String ENTRA_ROLES = "roles";
  private static final String OPERATOR = "role_operator";
  private static final String MANAGER = "role_manager";

  private static List<String> roles(Collection<GrantedAuthority> authorities) {
    return authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }

  @Test
  @DisplayName("Reads the Keycloak realm roles, the default claim")
  void readsKeycloakRealmRoles() {
    Map<String, Object> claims = Map.of("realm_access", Map.of(ENTRA_ROLES, List.of(OPERATOR, "offline_access")));

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of(REALM_ROLES)).convert(claims)))
      .containsExactly(OPERATOR, "offline_access");
  }

  @Test
  @DisplayName("Reads the Entra ID app roles from a top-level claim")
  void readsEntraIdRoles() {
    Map<String, Object> claims = Map.of(ENTRA_ROLES, List.of(MANAGER));

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of(ENTRA_ROLES)).convert(claims))).containsExactly(MANAGER);
  }

  @Test
  @DisplayName("Reads the Keycloak client roles through a nested path")
  void readsKeycloakClientRoles() {
    Map<String, Object> claims = Map.of("resource_access", Map.of("my-app", Map.of(ENTRA_ROLES, List.of(MANAGER))));

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of("resource_access.my-app.roles")).convert(claims)))
      .containsExactly(MANAGER);
  }

  @Test
  @DisplayName("Joins several claims in the configured order without duplicates")
  void joinsSeveralClaims() {
    Map<String, Object> claims = Map.of(ENTRA_ROLES, List.of(MANAGER, OPERATOR),
      "realm_access", Map.of(ENTRA_ROLES, List.of(OPERATOR, "role_admin")));

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of(ENTRA_ROLES, REALM_ROLES)).convert(claims)))
      .containsExactly(MANAGER, OPERATOR, "role_admin");
  }

  @Test
  @DisplayName("A claim name that exists as it is wins over the nested path, so URI claim names work")
  void exactClaimNameWins() {
    Map<String, Object> claims = Map.of("https://acme.com/roles", List.of(OPERATOR));

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of("https://acme.com/roles")).convert(claims)))
      .containsExactly(OPERATOR);
  }

  @Test
  @DisplayName("A single text value is one role")
  void textValueIsOneRole() {
    Map<String, Object> claims = Map.of(ENTRA_ROLES, OPERATOR);

    assertThat(roles(new ClaimRolesAuthoritiesConverter(List.of(ENTRA_ROLES)).convert(claims))).containsExactly(OPERATOR);
  }

  @Test
  @DisplayName("Missing claims, paths through non-objects, blank names and empty values give no role")
  void missingOrInvalidClaimsGiveNoRole() {
    Map<String, Object> claims = Map.of("realm_access", "not-an-object", "groups", Arrays.asList(null, " "));
    ClaimRolesAuthoritiesConverter converter = new ClaimRolesAuthoritiesConverter(
      Arrays.asList(REALM_ROLES, "missing.roles", "groups", " ", null));

    assertThat(converter.convert(claims)).isEmpty();
  }

  @Test
  @DisplayName("No configured claim gives no role")
  void noConfiguredClaim() {
    assertThat(new ClaimRolesAuthoritiesConverter(null).convert(Map.of(ENTRA_ROLES, List.of(OPERATOR)))).isEmpty();
  }
}
