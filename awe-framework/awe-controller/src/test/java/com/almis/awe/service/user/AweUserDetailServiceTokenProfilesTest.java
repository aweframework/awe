package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.dto.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * The roles of an identity provider token are mapped to AWE profiles as the single sign-on login maps them: the
 * authority prefix is stripped, only the names of existing profiles are kept (compared without case) and the main
 * profile follows the profile priority, else the first name in alphabetical order
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweUserDetailServiceTokenProfilesTest {

  private static final UserProfile OPERATOR = new UserProfile("OPE", "Operator", "operator");
  private static final UserProfile GENERAL = new UserProfile("GNR", "General", "general");

  @Mock
  BaseConfigProperties baseConfigProperties;
  @Mock
  UserDAO userDAO;

  private final SecurityConfigProperties securityConfigProperties = new SecurityConfigProperties();
  private AweUserDetailService userDetailsService;

  @BeforeEach
  void setUp() throws Exception {
    userDetailsService = new AweUserDetailService(baseConfigProperties, securityConfigProperties, userDAO);
    given(userDAO.findProfile(anyString())).willReturn(Optional.empty());
    given(userDAO.existRole(anyString())).willReturn(false);
    for (UserProfile profile : List.of(OPERATOR, GENERAL)) {
      for (String name : List.of(profile.name(), profile.name().toLowerCase(), profile.name().toUpperCase())) {
        given(userDAO.findProfile(name)).willReturn(Optional.of(profile));
        given(userDAO.existRole(name)).willReturn(true);
      }
    }
  }

  private static List<GrantedAuthority> roles(String... roles) {
    return Stream.of(roles).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
  }

  @Test
  void thePrefixIsStrippedAndTheValuesWithoutItOrWithoutAProfileAreIgnored() throws Exception {
    securityConfigProperties.getSso().setFilterAuthorityPrefix("AWE_");

    assertThat(userDetailsService.mapProfiles(roles("AWE_operator", "AWE_ghost", "General")))
      .containsExactly(OPERATOR);
  }

  @Test
  void theMainProfileFollowsTheProfilePriority() throws Exception {
    securityConfigProperties.getSso().setProfilePriority(List.of("ghost", "GENERAL"));

    assertThat(userDetailsService.mapProfiles(roles("Operator", "test", "General")))
      .containsExactly(GENERAL, OPERATOR);
  }

  @Test
  void withoutPriorityTheMainProfileIsTheFirstExistingNameInAlphabeticalOrder() throws Exception {
    assertThat(userDetailsService.mapProfiles(roles("Operator", "Aardvark", "General")))
      .containsExactly(GENERAL, OPERATOR);
  }

  @Test
  void anAcronymThatIsNotAProfileNameMapsToNoProfile() throws Exception {
    assertThat(userDetailsService.mapProfiles(roles("OPE"))).isEmpty();
    assertThat(userDetailsService.mapProfiles(roles())).isEmpty();
  }
}
