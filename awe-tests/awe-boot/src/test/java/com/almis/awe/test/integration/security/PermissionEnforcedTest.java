package com.almis.awe.test.integration.security;

import com.almis.awe.service.user.AweUserDetailService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The example of the ADR: a profile with {@code users.read} and {@code users.create} can create users but is refused
 * the deletion, while the administrator ({@code *}) can do both. The authorities are the ones a login loads from the
 * permissions assigned to the profiles (AweProPer), and the access by screen allows the users screen.
 */
@Tag("integration")
@DisplayName("Permission enforced tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
@TestPropertySource(properties = {"awe.security.authorization.mode=enforce", "awe.security.default-restriction=operator"})
class PermissionEnforcedTest extends ScreenAccessSupport {

  private static final String PREFIX = "permcreator";
  private static final String ACRONYM = "Q8P";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AweUserDetailService userDetailService;

  private int profile;

  @BeforeEach
  void createUsersCreatorProfile() {
    deleteRows();
    profile = jdbcTemplate.queryForObject("SELECT MAX(IdePro) FROM AwePro", Integer.class) + 1000;
    jdbcTemplate.update("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (?, ?, ?, 1, NULL, "
      + "'general', 1)", profile, ACRONYM, PREFIX);
    int permission = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(IdeAweProPer), 0) FROM AweProPer",
      Integer.class) + 1;
    for (String assigned : List.of("users.read", "users.create")) {
      jdbcTemplate.update("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) VALUES (?, ?, ?)", permission++, profile,
        assigned);
    }
  }

  @AfterEach
  void deleteRows() {
    jdbcTemplate.update("DELETE FROM ope WHERE l1_nom LIKE ?", PREFIX + "%");
    jdbcTemplate.update("DELETE FROM AweProPer WHERE IdePro IN (SELECT IdePro FROM AwePro WHERE Acr = ?)", ACRONYM);
    jdbcTemplate.update("DELETE FROM HISAweProPer WHERE IdePro IN (SELECT IdePro FROM AwePro WHERE Acr = ?)", ACRONYM);
    jdbcTemplate.update("DELETE FROM AwePro WHERE Acr = ?", ACRONYM);
  }

  @Test
  @DisplayName("A users creator can create a user but cannot delete it, and the administrator can")
  void usersCreatorCreatesButCannotDelete() throws Exception {
    Authentication creator = signedIn(userDetailService.getAuthorities(PREFIX, List.of(), List.of(ACRONYM)));
    Authentication administrator = signedIn(userDetailService.loadUserByUsername("test").getAuthorities());

    assertOk(maintain("UsrNew", newUser(PREFIX + "one"), creator));
    int created = userId(PREFIX + "one");

    assertRefused(maintain("UsrDel", deletedUser(created), creator), "UsrDel");
    assertThat(userCount(PREFIX + "one")).isOne();

    assertOk(maintain("UsrDel", deletedUser(created), administrator));
    assertThat(userCount(PREFIX + "one")).isZero();

    assertOk(maintain("UsrNew", newUser(PREFIX + "two"), administrator));
    assertThat(userCount(PREFIX + "two")).isOne();
  }

  @Test
  @DisplayName("The permissions of a profile change only with permissions.update, and are kept when unchanged")
  void changingTheProfilePermissionsRequiresPermissionsUpdate() throws Exception {
    Authentication reader = signedIn(AuthorityUtils.createAuthorityList("PERM_profiles.read", "PERM_profiles.update",
      "PERM_permissions.read"));
    Authentication updater = signedIn(AuthorityUtils.createAuthorityList("PERM_profiles.read", "PERM_profiles.update",
      "PERM_permissions.read", "PERM_permissions.update"));
    assertThat(permissionAccess(signedIn(AuthorityUtils.createAuthorityList("PERM_profiles.read")))).isEqualTo("none");
    assertThat(permissionAccess(reader)).isEqualTo("read");
    assertThat(permissionAccess(updater)).isEqualTo("update");

    assertRefused(maintain("ProUpd", profileUpdate("users.read", "users.delete"), reader), "ProUpd");
    assertThat(profilePermissions()).containsExactly("users.create", "users.read");
    assertThat(jdbcTemplate.queryForObject("SELECT Nam FROM AwePro WHERE IdePro = ?", String.class, profile).trim())
      .as("the refused update is not stored").isEqualTo(PREFIX);

    assertOk(maintain("ProUpd", profileUpdate("users.read", "users.create"), reader));
    assertOk(maintain("ProUpd", profileUpdate("users.read", "users.delete"), updater));
    assertThat(profilePermissions()).containsExactly("users.delete", "users.read");
  }

  private String permissionAccess(Authentication user) {
    try {
      return String.valueOf(JsonPath.<List<Object>>read(action("data/ProPerAcc", user),
        "$[?(@.type == 'fill')].parameters.datalist.rows[0].value").getFirst());
    } catch (Exception exc) {
      throw new IllegalStateException(exc);
    }
  }

  /**
   * Parameters of the profile panel of the profiles screen with the selected permissions
   */
  private ObjectNode profileUpdate(String... permissions) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.putArray("IdePro").add(profile);
    parameters.put("Acr", ACRONYM);
    parameters.put("Nam", PREFIX + " changed");
    parameters.put("IdeThm", 1);
    parameters.putNull("ScrIni");
    parameters.put("Res", "general");
    parameters.put("Act", 1);
    parameters.put("PerUpd", 1);
    ArrayNode selected = parameters.putArray("Per");
    for (String permission : permissions) {
      selected.add(permission);
    }
    parameters.put("max", 30);
    return parameters;
  }

  private List<String> profilePermissions() {
    return jdbcTemplate.queryForList("SELECT Per FROM AweProPer WHERE IdePro = ? ORDER BY Per", String.class, profile)
      .stream().map(String::trim).toList();
  }

  @Test
  @DisplayName("The service that dispatches a query is checked through the query: users.read for the list of users")
  void userListIsCheckedThroughTheQueryPermission() throws Exception {
    assertRefused(action("data/UsrLst", signedIn(List.of())), "UsrLst");

    String response = action("data/UsrLst", signedIn(AuthorityUtils.createAuthorityList("PERM_users.read")));
    assertThat(response).doesNotContain(REFUSED);
    assertThat(JsonPath.<List<Object>>read(response, "$[?(@.type == 'fill')]")).as(response).isNotEmpty();
  }

  @Test
  @DisplayName("An action whose service declares a permission is refused without it and runs with it")
  void serviceIsCheckedWithItsPermission() throws Exception {
    assertRefused(action("test-users-read", signedIn(List.of())), "TstUsrRea");

    String response = action("test-users-read", signedIn(AuthorityUtils.createAuthorityList("PERM_users.read")));
    assertThat(JsonPath.<List<String>>read(response, "$[*].type")).as(response).containsExactly("end-load");
  }

  @Test
  @DisplayName("The permissions view of the access window requires users.read and permissions.read")
  void permissionsViewRequiresUsersReadAndPermissionsRead() throws Exception {
    for (String query : List.of("access-permissions", "access-permission-counts", "access-permission-summary",
      "access-permission-why", "access-permission-why-result")) {
      assertRefused(action("data/" + query, signedIn(AuthorityUtils.createAuthorityList("PERM_users.read"))), query);
      assertRefused(action("data/" + query, signedIn(AuthorityUtils.createAuthorityList("PERM_permissions.read"))),
        query);
      assertThat(action("data/" + query, signedIn(AuthorityUtils.createAuthorityList("PERM_users.read",
        "PERM_permissions.read")))).as(query).doesNotContain(REFUSED);
    }
  }

  private String action(String path, Authentication user) throws Exception {
    return mockMvc.perform(post("/action/" + path)
        .with(csrf())
        .with(authentication(user))
        .session(session("administrator"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  @Test
  @DisplayName("A user without users.read opens the users screen without the list of users and with one refusal")
  void userWithoutUsersReadOpensTheUsersScreenWithoutTheList() throws Exception {
    String refused = screenData("users", signedIn(List.of()));

    assertThat(JsonPath.<List<Object>>read(refused, "$.components[?(@.id == 'GrdUsrAdm')].model.values[*]")).isEmpty();
    assertThat(JsonPath.<List<Object>>read(refused, "$.components[?(@.id == 'CrtUsrSta')].model.values[*]")).isEmpty();
    List<String> messages = JsonPath.read(refused, "$.actions[?(@.type == 'message')].parameters.message");
    assertThat(messages).as(refused).singleElement().asString().doesNotContain("UsrLst", "UsrStaCnt");
    assertThat(JsonPath.<List<String>>read(refused, "$.actions[?(@.type == 'message')].parameters.type"))
      .containsExactly("warning");

    String allowed = screenData("users", signedIn(userDetailService.loadUserByUsername("test").getAuthorities()));

    assertThat(JsonPath.<List<Object>>read(allowed, "$.components[?(@.id == 'GrdUsrAdm')].model.values[*]"))
      .isNotEmpty();
    assertThat(JsonPath.<List<Object>>read(allowed, "$.actions[?(@.type == 'message')]")).as(allowed).isEmpty();
  }

  @Test
  @DisplayName("A users.read only user gets the users screen without the buttons that change users and edits in view mode")
  void readerGetsTheUsersScreenWithoutTheButtonsThatChangeUsers() throws Exception {
    String[] changeButtons = {"ButNew", "CtxButDel", "ButDel", "CtxButUnl", "ButPnlUpd", "ButPnlNew"};

    String reader = screenData("users", signedIn(AuthorityUtils.createAuthorityList("PERM_users.read")));

    assertThat(JsonPath.<List<String>>read(reader, "$..id")).as(reader).doesNotContain(changeButtons)
      .contains("GrdUsrAdm", "CtxButUpd");
    assertThat(JsonPath.<List<Object>>read(reader, "$..[?(@.name == 'CtxSepDel')]")).as(reader).isEmpty();
    assertThat(viewModes(reader, "name", "EdtBtn")).containsOnly(true);
    assertThat(viewModes(reader, "id", "CtxButUpd")).containsOnly(true);

    String administrator = screenData("users", signedIn(userDetailService.loadUserByUsername("test").getAuthorities()));

    assertThat(JsonPath.<List<String>>read(administrator, "$..id")).contains(changeButtons);
    assertThat(JsonPath.<List<Object>>read(administrator, "$..[?(@.name == 'CtxSepDel')]")).isNotEmpty();
    assertThat(viewModes(administrator, "name", "EdtBtn")).containsOnlyNulls();
    assertThat(viewModes(administrator, "id", "CtxButUpd")).containsOnlyNulls();
  }

  /**
   * View mode flags of the dialog actions of an element (the openings of the user panel to edit)
   */
  private static List<Object> viewModes(String screen, String key, String value) {
    String path = "$..[?(@." + key + " == '" + value + "')].actions[?(@.type == 'dialog')]";
    List<Map<String, Object>> openings = JsonPath.read(screen, path);
    assertThat(openings).as(screen).isNotEmpty();
    return openings.stream().map(opening -> opening.get("viewMode")).toList();
  }

  @Test
  @DisplayName("A refused check of a unique user answers the refusal, not that the user already exists")
  void refusedUniqueCheckAnswersTheRefusal() throws Exception {
    String response = mockMvc.perform(post("/action/control-unique-cancel/UsrChkUni")
        .with(csrf())
        .with(authentication(signedIn(List.of())))
        .session(session("administrator"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"Usr\":\"" + PREFIX + "one\",\"target\":\"UniMsg\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertRefused(response, "UsrChkUni");
    assertThat(JsonPath.<List<String>>read(response, "$[?(@.type == 'message')].parameters.type"))
      .containsExactly("error");
    assertThat(JsonPath.<List<Object>>read(response, "$[?(@.type == 'message')].parameters.target")).isEmpty();
  }

  private String screenData(String option, Authentication user) throws Exception {
    return mockMvc.perform(post("/screen-data/" + option)
        .with(csrf())
        .with(authentication(user))
        .session(session("administrator"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  private static Authentication signedIn(Collection<GrantedAuthority> authorities) {
    return UsernamePasswordAuthenticationToken.authenticated("test", null, authorities);
  }

  private ObjectNode newUser(String name) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("Usr", name);
    parameters.put("Pas", "Secret.123");
    parameters.put("Sta", 1);
    parameters.put("Eml", name + "@almis.example");
    parameters.put("Lan", "en-GB");
    parameters.put("Nam", "Permission test user");
    parameters.put("Pro", profile);
    parameters.putArray("AddPro");
    parameters.put("AddProUpd", 1);
    parameters.put("max", 30);
    return parameters;
  }

  private ObjectNode deletedUser(int userId) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.putArray("IdeOpe").add(userId);
    return parameters;
  }

  private String maintain(String target, ObjectNode parameters, Authentication user) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .with(authentication(user))
        .session(session("administrator"))
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(parameters))
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  private int userId(String name) {
    return jdbcTemplate.queryForObject("SELECT IdeOpe FROM ope WHERE l1_nom = ?", Integer.class, name);
  }

  private int userCount(String name) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ope WHERE l1_nom = ?", Integer.class, name);
  }

  private static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }
}
