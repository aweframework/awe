package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Window access of a user with additional profiles: the union of the profiles, where allow wins, and the user
 * restrictions keep the last word. UsrStaCnt is a query of the users screen (option "users"), which the operator
 * profile file restricts and the administrator profile file allows.
 */
@Tag("integration")
@DisplayName("Additional profiles screen access tests")
@WithUserDetails(value = "test", userDetailsServiceBeanName = "aweUserDetailsService")
@TestPropertySource(properties = {"awe.security.authorization.mode=enforce", "awe.security.default-restriction=operator"})
class AdditionalProfilesScreenAccessTest extends ScreenAccessSupport {

  private static final String QUERY = "UsrStaCnt";
  private static final int FIRST_ID = 7710001;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @AfterEach
  void deleteRestrictions() {
    jdbcTemplate.update("delete from AweScrRes where IdeAweScrRes >= ?", FIRST_ID);
    jdbcTemplate.update("delete from AweOpeRol where IdeAweOpeRol >= ?", FIRST_ID);
    jdbcTemplate.update("update AwePro set Res = 'general' where Acr = 'GNR'");
  }

  @Test
  @DisplayName("A profile whose restriction file does not exist opens nothing")
  void missingRestrictionFileOpensNothing() throws Exception {
    jdbcTemplate.update("update AwePro set Res = 'missing-restriction-file' where Acr = 'GNR'");

    assertThat(query(session("OPE", "operator", "OPE,GNR"))).contains(REFUSED);
  }

  @Test
  @DisplayName("The modules of a user are the modules of every profile of the user")
  void modulesOfEveryProfile() throws Exception {
    // Test is a module of the administrator profile only, Base a module of every profile
    assertThat(modules(session("OPE", "operator", "OPE"))).contains("Base").doesNotContain("\"Test\"");

    jdbcTemplate.update("insert into AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) values (?, ?, ?)", FIRST_ID, user("test"),
      profile("ADM"));

    assertThat(modules(session("OPE", "operator", "OPE,ADM"))).contains("Base", "\"Test\"");
  }

  private String modules(MockHttpSession session) throws Exception {
    return mockMvc.perform(post("/action/data/ModNamByOpeSel")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  @Test
  @DisplayName("An operator with the administrator profile can use the users screen")
  void additionalProfileOpensTheScreen() throws Exception {
    assertThat(query(session("OPE", "operator", "OPE"))).contains(REFUSED);
    assertThat(query(session("OPE", "operator", "OPE,ADM"))).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("An additional profile that restricts the screen does not close it to an administrator")
  void additionalProfileDoesNotCloseTheScreen() throws Exception {
    assertThat(query(session("ADM", "administrator", "ADM,OPE"))).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("Between profile restrictions of the database, allow wins over restrict")
  void allowWinsBetweenProfileRestrictions() throws Exception {
    restrict(FIRST_ID, null, profile("ADM"), "R");
    restrict(FIRST_ID + 1, null, profile("OPE"), "A");

    assertThat(query(session("ADM", "administrator", "ADM"))).contains(REFUSED);
    assertThat(query(session("ADM", "administrator", "ADM,OPE"))).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A user restriction keeps the last word over every profile")
  void userRestrictionKeepsTheLastWord() throws Exception {
    restrict(FIRST_ID, user("test"), null, "R");

    assertThat(query(session("OPE", "operator", "OPE,ADM"))).contains(REFUSED);
  }

  @Test
  @DisplayName("The restriction file of the user keeps the last word over every profile")
  void userRestrictionFileKeepsTheLastWord() throws Exception {
    // The operator file restricts the users screen, which the administrator profile allows
    assertThat(query(session("OPE", "operator", "OPE,ADM", "operator"))).contains(REFUSED);
  }

  @Test
  @DisplayName("The main profile of a user with several profiles uses its own restriction file")
  void mainProfileUsesItsOwnRestrictionFile() throws Exception {
    // The restriction of the session is not the file of the main profile (administrator, which allows the screen)
    assertThat(query(session("ADM", "operator", "ADM,OPE"))).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A user restriction keeps the last word over the restriction file of the user")
  void userRestrictionKeepsTheLastWordOverTheUserFile() throws Exception {
    restrict(FIRST_ID, user("test"), null, "A");

    assertThat(query(session("OPE", "operator", "OPE,ADM", "operator"))).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A user whose restriction file does not exist opens nothing")
  void missingUserRestrictionFileOpensNothing() throws Exception {
    assertThat(query(session("OPE", "operator", "OPE,ADM", "missing-restriction-file"))).contains(REFUSED);
  }

  private String query(MockHttpSession session) throws Exception {
    return mockMvc.perform(post("/action/data/" + QUERY)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  private MockHttpSession session(String profile, String restriction, String profiles) throws Exception {
    return session(profile, restriction, profiles, null);
  }

  private MockHttpSession session(String profile, String restriction, String profiles, String userRestriction)
    throws Exception {
    MockHttpSession session = new MockHttpSession();
    String[][] parameters = userRestriction == null
      ? new String[][]{{"user", "test"}, {"profile", profile}, {"restriction", restriction}, {"profiles", profiles}}
      : new String[][]{{"user", "test"}, {"profile", profile}, {"restriction", restriction}, {"profiles", profiles},
        {"userRestriction", userRestriction}};
    for (String[] parameter : parameters) {
      mockMvc.perform(post("/session/set/" + parameter[0])
          .with(csrf())
          .param("value", parameter[1])
          .session(session))
        .andReturn();
    }
    return session;
  }

  private void restrict(int id, Integer user, Integer profile, String access) {
    jdbcTemplate.update("insert into AweScrRes (IdeAweScrRes, IdeOpe, IdePro, IdeMod, Opt, AccMod, Act) "
      + "values (?, ?, ?, null, 'users', ?, 1)", id, user, profile, access);
  }

  private Integer profile(String acronym) {
    return jdbcTemplate.queryForObject("select IdePro from AwePro where Acr = ?", Integer.class, acronym);
  }

  private Integer user(String name) {
    return jdbcTemplate.queryForObject("select IdeOpe from ope where l1_nom = ?", Integer.class, name);
  }
}
