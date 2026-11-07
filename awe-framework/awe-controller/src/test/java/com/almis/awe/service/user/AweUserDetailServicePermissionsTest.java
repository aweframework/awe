package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.ProfilePermission;
import com.almis.awe.model.entities.permissions.PermissionAction;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.permissions.PermissionResource;
import com.almis.awe.model.entities.permissions.PermissionVerb;
import com.almis.awe.model.entities.permissions.Permissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * Effective permissions of a user, loaded at login as PERM_ authorities: the union of the permissions of the main
 * and the additional profiles, expanded against the catalog, plus the permissions granted always
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweUserDetailServicePermissionsTest {

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
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getPermissionCatalog()).thenReturn(catalog());
    when(baseConfigProperties.getTheme()).thenReturn("sky");
    when(baseConfigProperties.getLanguageDefault()).thenReturn("en-GB");
    when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
    when(baseConfigProperties.getDefaultRole()).thenReturn("operator");
    given(userDAO.findByUserName("multi")).willReturn(new User()
      .setUsername("multi")
      .setEnabled(true)
      .setProfile("ADM")
      .setProfileName("administrator"));
    given(userDAO.findAdditionalProfiles("multi")).willReturn(List.of(new UserProfile("OPE", "operator", null)));
  }

  private static PermissionCatalog catalog() {
    return PermissionCatalog.build(List.of(new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("READ"),
        new PermissionVerb().setId("create").setLabel("CREATE"),
        new PermissionVerb().setId("update").setLabel("UPDATE")))
      .setResourceList(List.of(
        new PermissionResource().setId("users").setLabel("USERS").setVerbs("read,create")
          .setActionList(List.of(new PermissionAction().setId("unlock").setLabel("UNLOCK"))),
        new PermissionResource().setId("profiles").setLabel("PROFILES").setVerbs("read,update"),
        new PermissionResource().setId("self").setLabel("SELF").setVerbs("read").setGranted("always")))));
  }

  private List<String> authoritiesOf(String userName) {
    AweUserDetails details = userDetailsService.loadUserByUsername(userName);
    return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
  }

  /**
   * The permissions of the main and the additional profiles are joined, and the permissions granted always are added
   */
  @Test
  void loginLoadsTheUnionOfTheProfilePermissionsAndThePermissionsGrantedAlways() {
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("users.read"));
    given(userDAO.findPermissions(List.of("OPE"))).willReturn(List.of("profiles.update"));

    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator",
      "PERM_users.read", "PERM_profiles.update", "PERM_self.read");
  }

  /**
   * Wildcards are expanded against the catalog, and a permission granted by several profiles is added once
   */
  @Test
  void wildcardsAreExpandedWithoutDuplicates() {
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("users.*", "self.read"));
    given(userDAO.findPermissions(List.of("OPE"))).willReturn(List.of("*.read"));

    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator",
      "PERM_users.read", "PERM_users.create", "PERM_users.unlock", "PERM_self.read", "PERM_profiles.read");
  }

  /**
   * The whole catalog wildcard grants every permission
   */
  @Test
  void theWholeCatalogWildcardGrantsEveryPermission() {
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("*"));

    assertThat(authoritiesOf("multi")).filteredOn(authority -> authority.startsWith("PERM_"))
      .containsExactlyInAnyOrder("PERM_users.read", "PERM_users.create", "PERM_users.unlock", "PERM_profiles.read",
        "PERM_profiles.update", "PERM_self.read");
  }

  /**
   * An assignment that is not in the catalog, or a wildcard that matches nothing, is ignored
   */
  @Test
  void assignmentsMissingFromTheCatalogAreIgnored() {
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("invoices.read", "reports.*", "users.read"));

    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator", "PERM_users.read",
      "PERM_self.read");
  }

  /**
   * A user without assigned permissions keeps the permissions granted always
   */
  @Test
  void userWithoutAssignmentsGetsThePermissionsGrantedAlways() {
    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator", "PERM_self.read");
  }

  private static Profile profileFile(List<String> permissions, List<String> exclusions) {
    return new Profile()
      .setPermissions(permissions.stream().map(id -> new ProfilePermission().setId(id)).toList())
      .setExclusions(exclusions.stream().map(id -> new ProfilePermission().setId(id)).toList());
  }

  /**
   * Each profile gets the permissions of its profile file (AwePro.Res) minus the exclusions of that file, plus its
   * assignments: an exclusion only subtracts inside its profile, so another profile may still grant the permission
   */
  @Test
  void eachProfileGetsItsFileMinusItsExclusionsPlusItsAssignments() throws Exception {
    given(userDAO.findProfileRestrictions(List.of("ADM", "OPE")))
      .willReturn(Map.of("ADM", "contributor", "OPE", "reader"));
    given(aweElements.getProfile("contributor")).willReturn(profileFile(List.of("*"), List.of("users.*")));
    given(aweElements.getProfile("reader")).willReturn(profileFile(List.of("*.read"), List.of()));
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("users.unlock"));

    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator",
      "PERM_profiles.read", "PERM_profiles.update", "PERM_self.read", "PERM_users.unlock", "PERM_users.read");
  }

  /**
   * A profile without a restriction file takes the default one, as it does for the screen restrictions; a profile that
   * is not found gets no file permissions
   */
  @Test
  void aProfileWithoutRestrictionFileTakesTheDefaultFile() throws Exception {
    Map<String, String> files = new HashMap<>();
    files.put("ADM", null);
    given(userDAO.findProfileRestrictions(List.of("ADM", "OPE"))).willReturn(files);
    given(securityConfigProperties.getDefaultRestriction()).willReturn("general");
    given(aweElements.getProfile("general")).willReturn(profileFile(List.of("users.read"), List.of()));
    given(aweElements.getProfile("operator")).willReturn(profileFile(List.of("*"), List.of()));

    assertThat(authoritiesOf("multi")).containsExactly("ROLE_administrator", "ROLE_operator", "PERM_users.read",
      "PERM_self.read");
  }

  /**
   * The permission authorities of some profiles without a user are those the login loads for the same profiles: a
   * profile that is not an AWE profile grants nothing but the permissions granted always
   */
  @Test
  void permissionAuthoritiesOfProfilesWithoutUser() throws Exception {
    given(userDAO.findProfileRestrictions(List.of("ADM", "GHOST"))).willReturn(Map.of("ADM", "reader"));
    given(aweElements.getProfile("reader")).willReturn(profileFile(List.of("profiles.read"), List.of()));
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("users.read"));

    assertThat(userDetailsService.getPermissionAuthorities(List.of("ADM", "GHOST")))
      .extracting(GrantedAuthority::getAuthority)
      .containsExactlyInAnyOrder("PERM_profiles.read", "PERM_self.read", "PERM_users.read");
    assertThat(userDetailsService.getPermissionAuthorities(List.of("GHOST")))
      .extracting(GrantedAuthority::getAuthority)
      .containsExactly("PERM_self.read");
  }

  /**
   * Refreshing the additional profiles after a single sign-on login reloads the permissions too
   */
  @Test
  void refreshingTheAdditionalProfilesReloadsThePermissions() {
    given(userDAO.findPermissions(List.of("OPE"))).willReturn(List.of("profiles.read"));
    AweUserDetails details = new AweUserDetails().setUsername("multi").setProfile("ADM")
      .setProfileName("administrator");

    userDetailsService.refreshAdditionalProfiles(details);

    assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("ROLE_administrator", "ROLE_operator", "PERM_profiles.read", "PERM_self.read");
  }

  /**
   * The wildcards cover the default resource: *.read grants default.read, * grants default.write too, and the default
   * permissions are known assignments
   */
  @Test
  void wildcardsAndAssignmentsCoverTheDefaultResource() {
    when(aweElements.getPermissionCatalog()).thenReturn(PermissionCatalog.build(List.of(new Permissions()
      .setVerbList(List.of(new PermissionVerb().setId("read").setLabel("READ")))
      .setResourceList(List.of(new PermissionResource().setId("users").setLabel("USERS").setVerbs("read"),
        new PermissionResource().setId("default").setLabel("DEFAULT").setVerbs("read,write"))))));
    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("*.read"));
    given(userDAO.findPermissions(List.of("OPE"))).willReturn(List.of());

    assertThat(authoritiesOf("multi")).filteredOn(authority -> authority.startsWith("PERM_"))
      .containsExactly("PERM_users.read", "PERM_default.read");

    given(userDAO.findPermissions(List.of("ADM"))).willReturn(List.of("*"));
    assertThat(authoritiesOf("multi")).filteredOn(authority -> authority.startsWith("PERM_"))
      .containsExactly("PERM_users.read", "PERM_default.read", "PERM_default.write");

    given(userDAO.findAllPermissions()).willReturn(Map.of("OPE", List.of("default.read", "default.write",
      "default.*")));
    assertThat(userDetailsService.findUnknownPermissionAssignments()).isEmpty();
  }

  /**
   * The assignments of every profile that are not in the catalog are found at startup, with their profile
   */
  @Test
  void unknownAssignmentsAreFoundWithTheirProfile() {
    given(userDAO.findAllPermissions()).willReturn(Map.of("ADM", List.of("*", "invoices.read"),
      "OPE", List.of("users.read", "reports.*")));

    assertThat(userDetailsService.findUnknownPermissionAssignments())
      .containsExactlyInAnyOrderEntriesOf(Map.of("ADM", List.of("invoices.read"), "OPE", List.of("reports.*")));
  }
}
