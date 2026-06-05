package com.almis.awe.rest.autoconfigure.security;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AweRestJwtAuthenticationConverterTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

  private AweRestJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setDefaultRole("operator");

    SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();

    AweRestConfigProperties restConfigProperties = new AweRestConfigProperties();
    restConfigProperties.getOauth2ResourceServer().getPrincipal().setClientPrincipalPrefix("client:");

    converter = new AweRestJwtAuthenticationConverter(baseConfigProperties, securityConfigProperties, restConfigProperties);
  }

  @Test
  void shouldMapDelegatedTokenIntoSyntheticAweUserDetails() {
    Jwt jwt = Jwt.withTokenValue("delegated")
      .header("alg", "none")
      .claim("preferred_username", "user@example.com")
      .claim("name", "Delegated User")
      .claim("email", "user@example.com")
      .claim("sub", "sub-value")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

    assertThat(authenticationToken).isNotNull();
    assertThat(authenticationToken.getPrincipal()).isInstanceOf(AweUserDetails.class);

    AweUserDetails userDetails = (AweUserDetails) authenticationToken.getPrincipal();
    assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
    assertThat(userDetails.getName()).isEqualTo("Delegated User");
    assertThat(userDetails.getEmail()).isEqualTo("user@example.com");
    assertThat(userDetails.getProfile()).isEqualTo("operator");
    assertThat(userDetails.getAuthorities())
      .extracting("authority")
      .containsExactly("ROLE_operator");
  }

  @Test
  void shouldMapClientCredentialsTokenIntoSyntheticAweUserDetails() {
    Jwt jwt = Jwt.withTokenValue("client")
      .header("alg", "none")
      .claim("appid", "api-client-id")
      .claim("sub", "client-sub")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    assertThat(readConfiguredClientPrefix()).isEqualTo("client:");

    AbstractAuthenticationToken authenticationToken = converter.convert(jwt);
    AweUserDetails userDetails = (AweUserDetails) authenticationToken.getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo("client:api-client-id");
    assertThat(userDetails.getName()).isEqualTo("api-client-id");
    assertThat(userDetails.getEmail()).isNull();
    assertThat(userDetails.getProfile()).isEqualTo("operator");
  }

  @Test
  void shouldUseProfileClaimWhenConfiguredAndPresent() {
    AweRestConfigProperties configProperties = new AweRestConfigProperties();
    configProperties.getOauth2ResourceServer().getPrincipal().setProfileClaim("profile");
    configProperties.getOauth2ResourceServer().getPrincipal().setDefaultProfile("operator");

    converter = new AweRestJwtAuthenticationConverter(new BaseConfigProperties(), new SecurityConfigProperties(), configProperties);

    Jwt jwt = Jwt.withTokenValue("delegated")
      .header("alg", "none")
      .claim("preferred_username", "user@example.com")
      .claim("profile", "admin")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getProfile()).isEqualTo("admin");
    assertThat(userDetails.getProfileName()).isEqualTo("admin");
  }

  @Test
  void shouldFallbackToDefaultProfileWhenTokenHasNoProfileClaim() {
    AweRestConfigProperties configProperties = new AweRestConfigProperties();
    configProperties.getOauth2ResourceServer().getPrincipal().setDefaultProfile("rest-default");

    converter = new AweRestJwtAuthenticationConverter(new BaseConfigProperties(), new SecurityConfigProperties(), configProperties);

    Jwt jwt = Jwt.withTokenValue("delegated")
      .header("alg", "none")
      .claim("preferred_username", "user@example.com")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getProfile()).isEqualTo("rest-default");
    assertThat(userDetails.getProfileName()).isEqualTo("rest-default");
  }

  @Test
  void shouldRejectTokenWhenNoIdentityClaimsArePresent() {
    Jwt jwt = Jwt.withTokenValue("invalid")
      .header("alg", "none")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    assertThatThrownBy(() -> converter.convert(jwt))
      .isInstanceOf(OAuth2AuthenticationException.class)
      .hasMessageContaining("identity");
  }

  @Test
  void shouldResolveDelegatedIdentityFromAlternativeClaims() {
    Jwt jwt = Jwt.withTokenValue("delegated")
      .header("alg", "none")
      .claim("upn", "user-upn@example.com")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo("user-upn@example.com");
  }

  @Test
  void shouldResolveClientIdentityFromAlternativeClaims() {
    Jwt jwt = Jwt.withTokenValue("client")
      .header("alg", "none")
      .claim("azp", "client-azp")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo("client:client-azp");
  }

  @Test
  void shouldIgnoreBlankClaimValuesDuringResolution() {
    Jwt jwt = Jwt.withTokenValue("client")
      .header("alg", "none")
      .claim("preferred_username", "")
      .claim("client_id", "client-real")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT)
      .build();

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo("client:client-real");
  }

  @Test
  void shouldNotDependOnUserLookupOrProvisioningServices() {
    List<Constructor<?>> constructors = Arrays.stream(AweRestJwtAuthenticationConverter.class.getDeclaredConstructors())
      .toList();

    assertThat(constructors)
      .isNotEmpty()
      .allSatisfy(this::assertConstructorDoesNotDependOnForbiddenServices);
  }

  private String readConfiguredClientPrefix() {
    try {
      Field restConfigField = AweRestJwtAuthenticationConverter.class.getDeclaredField("restConfigProperties");
      restConfigField.setAccessible(true);
      AweRestConfigProperties configProperties = (AweRestConfigProperties) restConfigField.get(converter);
      return configProperties.getOauth2ResourceServer().getPrincipal().getClientPrincipalPrefix();
    } catch (ReflectiveOperationException exc) {
      throw new IllegalStateException(exc);
    }
  }

  private void assertConstructorDoesNotDependOnForbiddenServices(Constructor<?> constructor) {
    List<String> forbiddenTypeNames = List.of(
      "org.springframework.security.core.userdetails.UserDetailsService",
      "com.almis.awe.dao.UserDAO",
      "com.almis.awe.service.AccessService",
      "com.almis.awe.service.MaintainService"
    );

    for (Parameter parameter : constructor.getParameters()) {
      assertThat(parameter.getType().getName())
        .isNotIn(forbiddenTypeNames);
    }
  }
}
