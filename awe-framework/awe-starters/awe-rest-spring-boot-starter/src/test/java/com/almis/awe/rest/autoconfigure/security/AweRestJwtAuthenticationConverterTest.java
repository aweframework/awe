package com.almis.awe.rest.autoconfigure.security;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.permissions.PermissionResource;
import com.almis.awe.model.entities.permissions.PermissionVerb;
import com.almis.awe.model.entities.permissions.Permissions;
import com.almis.awe.rest.autoconfigure.config.AweRestConfigProperties;
import com.almis.awe.service.user.AweUserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AweRestJwtAuthenticationConverterTest {

  private static final Instant TOKEN_ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
  private static final Instant TOKEN_EXPIRES_AT = Instant.parse("2026-01-01T00:05:00Z");

  /**
   * AWE profiles by name in lower case: Administrator (ADM) is assigned users.*, Operator (OPE) profiles.read and
   * General (GNR) nothing. Names are found without case, as the single sign-on finds them
   */
  private static final Map<String, UserProfile> PROFILES = Map.of(
    "administrator", new UserProfile("ADM", "Administrator", "administrator"),
    "operator", new UserProfile("OPE", "Operator", null),
    "general", new UserProfile("GNR", "General", "general"));

  private final BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
  private final SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
  private final AweRestConfigProperties restConfigProperties = new AweRestConfigProperties();
  private final UserDAO userDAO = mock(UserDAO.class);

  private AweRestJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() throws Exception {
    baseConfigProperties.setDefaultRole("operator");
    securityConfigProperties.setDefaultRestriction("restricted");
    restConfigProperties.getOauth2ResourceServer().getPrincipal().setClientPrincipalPrefix("client:");

    when(userDAO.findProfile(anyString())).thenAnswer(invocation ->
      Optional.ofNullable(PROFILES.get(invocation.<String>getArgument(0).toLowerCase(Locale.ROOT))));
    when(userDAO.existRole(anyString())).thenAnswer(invocation ->
      PROFILES.containsKey(invocation.<String>getArgument(0).toLowerCase(Locale.ROOT)));
    when(userDAO.findProfileRestrictions(anyList()))
      .thenAnswer(invocation -> invocation.<List<String>>getArgument(0).stream()
        .collect(Collectors.toMap(profile -> profile, profile -> "missing")));
    when(userDAO.findPermissions(List.of("ADM"))).thenReturn(List.of("users.*"));
    when(userDAO.findPermissions(List.of("OPE"))).thenReturn(List.of("profiles.read"));
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getPermissionCatalog()).thenReturn(catalog());
    AweUserDetailService userDetailService = new AweUserDetailService(baseConfigProperties, securityConfigProperties,
      userDAO);
    userDetailService.setApplicationContext(context);

    converter = new AweRestJwtAuthenticationConverter(baseConfigProperties, securityConfigProperties,
      restConfigProperties, () -> userDetailService);
  }

  private static PermissionCatalog catalog() {
    return PermissionCatalog.build(List.of(new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("READ"),
        new PermissionVerb().setId("create").setLabel("CREATE")))
      .setResourceList(List.of(
        new PermissionResource().setId("users").setLabel("USERS").setVerbs("read,create"),
        new PermissionResource().setId("profiles").setLabel("PROFILES").setVerbs("read"),
        new PermissionResource().setId("self").setLabel("SELF").setVerbs("read").setGranted("always")))));
  }

  private static Jwt.Builder delegatedToken() {
    return Jwt.withTokenValue("delegated")
      .header("alg", "none")
      .claim("preferred_username", "user@example.com")
      .claim("sub", "subject")
      .issuedAt(TOKEN_ISSUED_AT)
      .expiresAt(TOKEN_EXPIRES_AT);
  }

  private AweUserDetails convertProfiles(Object claimValue) {
    restConfigProperties.getOauth2ResourceServer().getPrincipal().setProfileClaim("profiles");
    return (AweUserDetails) converter.convert(delegatedToken().claim("profiles", claimValue).build()).getPrincipal();
  }

  private static List<String> authorities(AweUserDetails userDetails) {
    return userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
  }

  @Test
  void shouldMapDelegatedTokenIntoSyntheticAweUserDetails() {
    Jwt jwt = delegatedToken()
      .claim("name", "Delegated User")
      .claim("email", "user@example.com")
      .build();

    AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

    assertThat(authenticationToken).isNotNull();
    assertThat(authenticationToken.getPrincipal()).isInstanceOf(AweUserDetails.class);

    AweUserDetails userDetails = (AweUserDetails) authenticationToken.getPrincipal();
    assertThat(userDetails.getUsername()).isEqualTo("user@example.com");
    assertThat(userDetails.getName()).isEqualTo("Delegated User");
    assertThat(userDetails.getEmail()).isEqualTo("user@example.com");
    // Without a profile claim the default role is the profile, with its permissions
    assertThat(userDetails.getProfile()).isEqualTo("OPE");
    assertThat(userDetails.getProfileName()).isEqualTo("Operator");
    assertThat(userDetails.getRestrictions()).isEqualTo("restricted");
    assertThat(authorities(userDetails)).containsExactlyInAnyOrder("ROLE_Operator", "PERM_profiles.read",
      "PERM_self.read");
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

    AweUserDetails userDetails = (AweUserDetails) converter.convert(jwt).getPrincipal();

    assertThat(userDetails.getUsername()).isEqualTo("client:api-client-id");
    assertThat(userDetails.getName()).isEqualTo("api-client-id");
    assertThat(userDetails.getEmail()).isNull();
    assertThat(userDetails.getProfile()).isEqualTo("OPE");
  }

  @Test
  void shouldMapTheProfileNameOfTheClaimWithoutCaseToItsAweProfileAndPermissions() {
    AweUserDetails userDetails = convertProfiles("ADMINISTRATOR");

    assertThat(userDetails.getProfile()).isEqualTo("ADM");
    assertThat(userDetails.getProfileName()).isEqualTo("Administrator");
    assertThat(userDetails.getRestrictions()).isEqualTo("administrator");
    assertThat(authorities(userDetails)).containsExactlyInAnyOrder("ROLE_Administrator", "PERM_users.read",
      "PERM_users.create", "PERM_self.read");
  }

  @Test
  void shouldGiveOneRolePerExistingProfileAndIgnoreTheValuesWithoutProfile() {
    AweUserDetails userDetails = convertProfiles(List.of("", "operator", "ghost", "administrator", "operator"));

    // Without priority the main profile is the first existing name in alphabetical order
    assertThat(userDetails.getProfile()).isEqualTo("ADM");
    assertThat(userDetails.getProfiles()).containsExactly("ADM", "OPE");
    assertThat(authorities(userDetails)).containsExactlyInAnyOrder("ROLE_Administrator", "ROLE_Operator",
      "PERM_users.read", "PERM_users.create", "PERM_profiles.read", "PERM_self.read");
  }

  @Test
  void shouldUseTheProfilePriorityToChooseTheMainProfile() {
    securityConfigProperties.getSso().setProfilePriority(List.of("ghost", "OPERATOR"));

    AweUserDetails userDetails = convertProfiles(List.of("administrator", "operator"));

    assertThat(userDetails.getProfile()).isEqualTo("OPE");
    assertThat(userDetails.getProfileName()).isEqualTo("Operator");
    assertThat(userDetails.getProfiles()).containsExactly("OPE", "ADM");
  }

  @Test
  void shouldStripTheAuthorityPrefixAndIgnoreTheValuesWithoutIt() {
    securityConfigProperties.getSso().setFilterAuthorityPrefix("AWE_");

    AweUserDetails userDetails = convertProfiles(List.of("AWE_general", "administrator"));

    assertThat(userDetails.getProfiles()).containsExactly("GNR");
    assertThat(authorities(userDetails)).containsExactly("ROLE_General", "PERM_self.read");
  }

  @Test
  void shouldReadTheProfilesFromANestedClaim() {
    restConfigProperties.getOauth2ResourceServer().getPrincipal().setProfileClaim("realm_access.roles");

    AweUserDetails userDetails = (AweUserDetails) converter.convert(delegatedToken()
      .claim("realm_access", Map.of("roles", List.of("General", "offline_access")))
      .build()).getPrincipal();

    assertThat(userDetails.getProfiles()).containsExactly("GNR");
  }

  @Test
  void shouldGiveTheDefaultProfileWhenNoValueOfTheClaimIsAProfileName() {
    // ADM is an acronym, not a profile name: it grants nothing
    AweUserDetails userDetails = convertProfiles(List.of("ADM", "ghost"));

    assertThat(userDetails.getProfiles()).containsExactly("OPE");
    assertThat(authorities(userDetails)).containsExactlyInAnyOrder("ROLE_Operator", "PERM_profiles.read",
      "PERM_self.read");
  }

  @Test
  void shouldRejectTheTokenWhenTheProfilesCannotBeRead() throws Exception {
    when(userDAO.findProfile(anyString())).thenThrow(new AWException("error", "no table"));

    // A failed lookup refuses the token: it never falls back to the default profile and its permissions
    assertThatThrownBy(() -> convertProfiles(List.of("Administrator")))
      .isInstanceOf(AuthenticationServiceException.class);
    assertThatThrownBy(() -> converter.convert(delegatedToken().build()))
      .isInstanceOf(AuthenticationServiceException.class);
  }

  @Test
  void shouldRejectTheTokenWhenARoleIsRequiredAndNoValueOfTheClaimIsAProfileName() {
    securityConfigProperties.getSso().setRequireRole(true);

    assertThatThrownBy(() -> convertProfiles(List.of("ADM", "ghost")))
      .isInstanceOf(OAuth2AuthenticationException.class)
      .hasMessageContaining("profile");
  }

  @Test
  void shouldKeepTheDefaultProfileWithoutAProfileClaimEvenWhenARoleIsRequired() {
    securityConfigProperties.getSso().setRequireRole(true);

    AweUserDetails userDetails = (AweUserDetails) converter.convert(delegatedToken().build()).getPrincipal();

    assertThat(userDetails.getProfile()).isEqualTo("OPE");
  }

  @Test
  void shouldKeepAConfiguredDefaultProfileThatIsNotAnAweProfileWithoutItsPermissions() {
    restConfigProperties.getOauth2ResourceServer().getPrincipal().setDefaultProfile("rest-default");

    AweUserDetails userDetails = (AweUserDetails) converter.convert(delegatedToken().build()).getPrincipal();

    assertThat(userDetails.getProfile()).isEqualTo("rest-default");
    assertThat(userDetails.getProfileName()).isEqualTo("rest-default");
    assertThat(authorities(userDetails)).containsExactly("ROLE_rest-default", "PERM_self.read");
  }

  @Test
  void shouldResolveThePermissionsOfTheProfilesOnEveryConversion() {
    convertProfiles("administrator");
    when(userDAO.findPermissions(List.of("ADM"))).thenReturn(List.of("profiles.read"));

    AweUserDetails userDetails = convertProfiles("administrator");

    assertThat(authorities(userDetails)).containsExactlyInAnyOrder("ROLE_Administrator", "PERM_profiles.read",
      "PERM_self.read");
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
