package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.dto.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

@ExtendWith(MockitoExtension.class)
class AweUserDetailServiceTest {

  @InjectMocks
  AweUserDetailService userDetailsService;
  @Mock
  BaseConfigProperties baseConfigProperties;
  @Mock
  SecurityConfigProperties securityConfigProperties;
  @Mock
  ApplicationContext context;
  @Mock
  AweElements aweElements;
  @Mock
  UserDAO userDAO;

  @BeforeEach
  void setUp() {
    userDetailsService.setApplicationContext(context);
  }

  @Test
  void giveSomeUser_willReturnUserDetails() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLastChangedPasswordDate(new Date())
      .setLocked(true));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertTrue(details.isCredentialsNonExpired()),
      () -> assertFalse(details.isAccountNonLocked())
    );
  }

  @Test
  void givenPwdExpAndPasswordChangedRecently_credentialsAreNotExpired() {
    assertCredentialsNonExpired("30", daysAgo(5), true);
  }

  @Test
  void givenPwdExpAndPasswordChangedLongAgo_credentialsAreExpired() {
    assertCredentialsNonExpired("30", daysAgo(31), false);
  }

  @Test
  void givenPwdExpAndPasswordChangedJustInsideTheLimit_credentialsAreNotExpired() {
    // The password is valid until PwdExp days after the change: two hours before that moment it is still valid
    assertCredentialsNonExpired("30", daysAgo(30).plusHours(2), true);
  }

  @Test
  void givenPwdExpAndPasswordChangedJustOutsideTheLimit_credentialsAreExpired() {
    // Two hours after PwdExp days since the change the password is expired
    assertCredentialsNonExpired("30", daysAgo(30).minusHours(2), false);
  }

  @Test
  void givenPwdExpAndPasswordNeverChanged_credentialsAreExpired() {
    // A user without a last change date (new or reset by an administrator) must change the password when PwdExp is set
    assertCredentialsNonExpired("30", null, false);
  }

  @Test
  void givenNoPwdExp_passwordsNeverExpire() {
    assertCredentialsNonExpired(null, daysAgo(10000), true);
  }

  @Test
  void givenNoPwdExpAndPasswordNeverChanged_credentialsAreNotExpired() {
    assertCredentialsNonExpired(null, null, true);
  }

  private LocalDateTime daysAgo(int days) {
    return LocalDateTime.now().minusDays(days);
  }

  private void assertCredentialsNonExpired(String pwdExp, LocalDateTime lastChange, boolean expectedNonExpired) {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn(pwdExp);
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLastChangedPasswordDate(lastChange == null ? null : Date.from(lastChange.atZone(ZoneId.systemDefault()).toInstant()))
      .setLocked(false));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertEquals(expectedNonExpired, details.isCredentialsNonExpired());
  }

  @Test
  void userWithAdditionalProfiles_getsOneRolePerProfileAndKeepsTheSettingsOfTheMainProfile() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    given(userDAO.findByUserName("multi")).willReturn(new User()
      .setUsername("multi")
      .setEnabled(true)
      .setProfile("ADM")
      .setProfileName("administrator")
      .setProfileRestriction("general")
      .setProfileTheme("sunset")
      .setProfileInitialScreen("home"));
    given(userDAO.findAdditionalProfiles("multi")).willReturn(List.of(
      new UserProfile("OPE", "operator", "operator"),
      new UserProfile("AUD", "auditor", null)));

    AweUserDetails details = (AweUserDetails) userDetailsService.loadUserByUsername("multi");

    assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("ROLE_administrator", "ROLE_operator", "ROLE_auditor");
    assertThat(details.getProfile()).isEqualTo("ADM");
    assertThat(details.getRestrictions()).isEqualTo("general");
    assertThat(details.getTheme()).isEqualTo("sunset");
    assertThat(details.getInitialScreen()).isEqualTo("home");
    assertThat(details.getProfiles()).containsExactly("ADM", "OPE", "AUD");
    assertThat(details.getProfileRestrictions()).containsExactly("general", "operator");
    assertThat(details.getUserRestriction()).isNull();
  }

  @Test
  void userWithoutAdditionalProfiles_keepsOneRoleAndOneProfile() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    given(userDAO.findByUserName("single")).willReturn(new User()
      .setUsername("single")
      .setEnabled(true)
      .setProfile("ADM")
      .setProfileName("administrator")
      .setUserRestriction("own"));

    AweUserDetails details = (AweUserDetails) userDetailsService.loadUserByUsername("single");

    assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("ROLE_administrator");
    assertThat(details.getProfiles()).containsExactly("ADM");
    assertThat(details.getProfileRestrictions()).containsExactly("own");
    assertThat(details.getUserRestriction()).isEqualTo("own");
  }

  private void mockProperties() {
    when(baseConfigProperties.getTheme()).thenReturn("sky");
    when(baseConfigProperties.getLanguageDefault()).thenReturn("es-ES");
    when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
    when(baseConfigProperties.getDefaultRole()).thenReturn("operator");
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("manager");
  }

  @Test
  void giveSomeUserWithCredentialExpired_willReturnUserDetails() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLocked(false));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertFalse(details.isCredentialsNonExpired()),
      () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void testGetAuthorities() {
    GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_DUMMY");
    List<GrantedAuthority> expectedValue = Collections.singletonList(grantedAuthority);
    assertEquals(expectedValue, userDetailsService.getAuthorities("DUMMY"));
  }

  @Test
  void testMapGrantedAuthorityProfile_withAuthoritiesAndPrefix() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals(Optional.of("MANAGER"), result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withSeveralMatchingRoles_picksTheFirstProfileInAlphabeticalOrder() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    List<GrantedAuthority> oneOrder = List.of(new SimpleGrantedAuthority("ROLE_operator"),
      new SimpleGrantedAuthority("offline_access"), new SimpleGrantedAuthority("ROLE_manager"));
    List<GrantedAuthority> otherOrder = List.of(new SimpleGrantedAuthority("ROLE_manager"),
      new SimpleGrantedAuthority("ROLE_operator"));

    assertEquals(Optional.of("manager"), userDetailsService.mapGrantedAuthorityProfile(oneOrder));
    assertEquals(Optional.of("manager"), userDetailsService.mapGrantedAuthorityProfile(otherOrder));
  }

  @Test
  void testMapGrantedAuthorityProfile_withSeveralMatchingRoles_picksTheFirstExistingProfile() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    given(userDAO.existRole("auditor")).willReturn(false);
    given(userDAO.existRole("manager")).willReturn(true);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_manager"),
      new SimpleGrantedAuthority("ROLE_auditor"), new SimpleGrantedAuthority("ROLE_operator"));

    assertEquals(Optional.of("manager"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  @Test
  void testMapGrantedAuthorityProfile_withSeveralMatchingRolesAndNoProfile_picksTheFirstInAlphabeticalOrder() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    given(userDAO.existRole(anyString())).willReturn(false);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_operator"),
      new SimpleGrantedAuthority("ROLE_auditor"));

    assertEquals(Optional.of("auditor"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  @Test
  void testMapGrantedAuthorityProfile_withProfilePriority_picksTheFirstSentAndExistingProfileOfTheList()
    throws AWException {
    mockSsoWithProfilePriority("administrator", "ghost", "manager", "operator");
    mockExistingProfiles("administrator", "auditor", "manager", "operator");
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_operator"),
      new SimpleGrantedAuthority("ROLE_ghost"), new SimpleGrantedAuthority("ROLE_auditor"),
      new SimpleGrantedAuthority("ROLE_manager"));

    // administrator is not sent and ghost has no profile: manager wins over auditor (alphabetical) and operator
    assertEquals(Optional.of("manager"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  @Test
  void testMapGrantedAuthorityProfile_withProfilePriority_comparesWithoutCase() throws AWException {
    mockSsoWithProfilePriority("OPERATOR");
    mockExistingProfiles("auditor", "operator");
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_auditor"),
      new SimpleGrantedAuthority("ROLE_operator"));

    assertEquals(Optional.of("operator"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  @Test
  void testMapGrantedAuthorityProfile_withProfilePriorityNotSent_picksTheFirstExistingInAlphabeticalOrder()
    throws AWException {
    mockSsoWithProfilePriority("administrator");
    mockExistingProfiles("administrator", "auditor", "operator");
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_operator"),
      new SimpleGrantedAuthority("ROLE_auditor"));

    assertEquals(Optional.of("auditor"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  private void mockSsoWithProfilePriority(String... profiles) {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    ssoProperties.setProfilePriority(List.of(profiles));
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
  }

  private void mockExistingProfiles(String... profiles) throws AWException {
    List<String> existing = List.of(profiles);
    given(userDAO.existRole(anyString()))
      .willAnswer(invocation -> existing.contains(invocation.<String>getArgument(0)));
  }

  @Test
  void testMapGrantedAuthorityProfile_withASingleMatchingRole_doesNotLookUpTheProfile() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);

    assertEquals(Optional.of("operator"), userDetailsService.mapGrantedAuthorityProfile(List.of(new SimpleGrantedAuthority("ROLE_operator"))));
    verify(userDAO, never()).existRole(anyString());
  }

  @Test
  void testMapGrantedAuthorityProfile_skipsAuthoritiesThatCarryOnlyThePrefix() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_"), new SimpleGrantedAuthority("ROLE_operator"));

    assertEquals(Optional.of("operator"), userDetailsService.mapGrantedAuthorityProfile(authorities));
  }

  @Test
  void testMapGrantedAuthorityProfile_withNullAuthorities_returnEmpty() throws AWException {
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(null);
    assertEquals(Optional.empty(), result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withEmptyAuthorities_returnEmpty() throws AWException {
    List<GrantedAuthority> authorities = Collections.emptyList();
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals(Optional.empty(), result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withNullPrefixFilter_returnFullRole() throws AWException {
    SecurityConfigProperties.Sso ssoConfig = new SecurityConfigProperties.Sso();
    ssoConfig.setFilterAuthorityPrefix(null);
    when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals(Optional.of("ROLE_ADMIN"), result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withNoAuthorityMatchingPrefix_returnEmpty() throws AWException {
    SecurityConfigProperties.Sso ssoConfig = new SecurityConfigProperties.Sso();
    ssoConfig.setFilterAuthorityPrefix("ROLE_ADMINS");
    when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals(Optional.empty(), result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withAuthorityEqualToPrefix_returnEmpty() throws AWException {
    SecurityConfigProperties.Sso ssoConfig = new SecurityConfigProperties.Sso();
    ssoConfig.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_"));
    Optional<String> result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals(Optional.empty(), result);
  }


  @Test
  void loadUserByEmail() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByEmail(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    UserDetails details = userDetailsService.loadUserByEmail("foo@dummy.com");
    assertAll(
        () -> assertNotNull(details),
        () -> assertFalse(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void givenRole_loadUserByRole(boolean existRole) throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(securityConfigProperties.getSso()).thenReturn(new SecurityConfigProperties.Sso());
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(oAuth2User, grantedAuthorities, "clientRegId");
    given(userDAO.findByRole(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEmail("test@acme.com")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    given(userDAO.existRole(anyString())).willReturn(existRole);
    UserDetails details = userDetailsService.loadUserByRole(oAuth2AuthenticationToken);
    assertAll(
        () -> assertNotNull(details),
        () -> assertTrue(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void givenNoAuthorities_loadUserByRole_usesDefaultRole() throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
        List.of(new OAuth2UserAuthority(attributeMap)), attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(oAuth2User, Collections.emptyList(), "clientRegId");
    given(userDAO.findByRole("operator")).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEmail("test@acme.com")
        .setEnabled(true)
        .setProfile("operator")
        .setLocked(false));
    given(userDAO.existRole("operator")).willReturn(true);
    UserDetails details = userDetailsService.loadUserByRole(oAuth2AuthenticationToken);
    assertAll(
        () -> assertNotNull(details),
        () -> assertEquals("operator", ((AweUserDetails) details).getProfileName())
    );
  }

  @Test
  void givenSeveralRolesAndProfilePriority_loadUserByRole_usesTheProfileOfTheList() throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    mockSsoWithProfilePriority("manager");
    mockExistingProfiles("auditor", "manager");
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new SimpleGrantedAuthority("ROLE_auditor"),
      new SimpleGrantedAuthority("ROLE_manager"));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(oAuth2User, grantedAuthorities, "clientRegId");
    given(userDAO.findByRole("manager")).willReturn(new User().setProfile("MNG").setProfileName("manager"));

    AweUserDetails details = userDetailsService.loadUserByRole(token);

    assertEquals("manager", details.getProfileName());
  }

  @Test
  void givenNullRole_loadDefaultUserRole() throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(securityConfigProperties.getSso()).thenReturn(new SecurityConfigProperties.Sso());
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(oAuth2User, grantedAuthorities, "clientRegId");
    given(userDAO.findByRole(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEmail("test@acme.com")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    given(userDAO.existRole(anyString())).willReturn(true);
    UserDetails details = userDetailsService.loadUserByRole(oAuth2AuthenticationToken);
    assertAll(
        () -> assertNotNull(details),
        () -> assertTrue(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void mapAdditionalProfileIds_returnsEveryOtherMappedRoleWithAProfile() throws AWException {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    given(userDAO.findProfileId("auditor")).willReturn(Optional.of(1));
    given(userDAO.findProfileId("ghost")).willReturn(Optional.empty());
    given(userDAO.findProfileId("manager")).willReturn(Optional.of(2));
    given(userDAO.findProfileId("operator")).willReturn(Optional.of(3));
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_operator"),
      new SimpleGrantedAuthority("offline_access"), new SimpleGrantedAuthority("ROLE_ghost"),
      new SimpleGrantedAuthority("ROLE_auditor"), new SimpleGrantedAuthority("ROLE_AUDITOR"),
      new SimpleGrantedAuthority("ROLE_manager"));

    // The main profile (auditor, in any case) is not an additional profile, and a role without a profile is ignored
    assertThat(userDetailsService.mapAdditionalProfileIds(authorities, "auditor")).containsExactly(2, 3);
  }

  @Test
  void mapAdditionalProfileIds_withoutAuthorities_returnsNone() throws AWException {
    assertThat(userDetailsService.mapAdditionalProfileIds(null, "auditor")).isEmpty();
    assertThat(userDetailsService.mapAdditionalProfileIds(Collections.emptyList(), "auditor")).isEmpty();
  }

  @Test
  void refreshAdditionalProfiles_reloadsTheAdditionalProfilesWithOneRolePerProfile() {
    given(userDAO.findAdditionalProfiles("multi")).willReturn(List.of(new UserProfile("OPE", "operator", "operator")));
    AweUserDetails details = new AweUserDetails().setUsername("multi").setProfile("ADM").setProfileName("administrator")
      .setAuthorities(userDetailsService.getAuthorities("administrator"));

    userDetailsService.refreshAdditionalProfiles(details);

    assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("ROLE_administrator", "ROLE_operator");
    assertThat(details.getProfiles()).containsExactly("ADM", "OPE");
  }

  @Test
  void supportsAdditionalProfiles_isTheSupportOfTheUserDAO() {
    given(userDAO.supportsAdditionalProfiles()).willReturn(false, true);

    assertThat(userDetailsService.supportsAdditionalProfiles()).isFalse();
    assertThat(userDetailsService.supportsAdditionalProfiles()).isTrue();
  }
}
