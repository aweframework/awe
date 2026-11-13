package com.almis.awe.test.integration.security;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("Security Tests")
class SecurityIntegrationTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private AweElements elements;

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePublicQuery_should200Ok() throws Exception {
    mockMvc.perform(post("/action/data/GetCountries")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePrivateQuery_should401Unauthorized() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$[0].type").value("end-load"))
      .andExpect(jsonPath("$[1].type").value("message"))
      .andExpect(jsonPath("$[1].parameters.title").isNotEmpty())
      .andExpect(jsonPath("$[1].parameters.message").isNotEmpty());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUser_screenDataOfAPrivateOption_should401WithTheSessionExpiredAnswer() throws Exception {
    // Without a session a private option is an expired session, not a missing screen: the client shows the panel and
    // not the "screen not found" page (issue #935)
    mockMvc.perform(post("/screen-data/users")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.title").value("Session expired"));
  }

  @Test
  @DisplayName("The generic screens bring their own locale keys and never redefine one of another module")
  void genericScreensDoNotRedefineTheRestartButtonOfTheScheduler() {
    // The scheduler management screen owns BUTTON_RESTART: the session expired panel needs its own key, because the
    // locales of every module are merged and the first definition of a key wins
    assertThat(elements.getLocaleWithLanguage("BUTTON_RESTART", "en-GB")).isEqualTo("Restart");
    assertThat(elements.getLocaleWithLanguage("BUTTON_RESTART_APPLICATION", "en-GB"))
      .isEqualTo("Reload and go to start");
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_executePublicMaintain_should200Ok() throws Exception {
    mockMvc.perform(post("/action/maintain/CleanUpSequence")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePrivateMaintain_should401NotAuthorized() throws Exception {
    mockMvc.perform(post("/action/maintain/CleanUp")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {
    // awe-developer
    "data/LitTra", "data/LitLst", "data/GetTraSer", "data/GetSelectedLocale", "data/SwitchLanguagesTxt",
    "data/SwitchLanguagesMrk", "data/checkPath",
    "maintain/LitTra", "maintain/LitSav", "maintain/LitNew", "maintain/LitDel", "maintain/SetPath",
    // awe-scheduler
    "data/taskDependencies", "data/taskParameters", "data/taskVariableParameters", "data/taskHasVariableParameters",
    "data/SchCal", "data/SchCalDatLst",
    "maintain/ActSchTsk", "maintain/DeaSchTsk",
    // awe-tools
    "maintain/selectExtract", "maintain/SaveSelectFile", "maintain/DelSqlCol"})
  @WithAnonymousUser
  @DisplayName("An administration target of the modules needs a session")
  void givenAnonymousUserAndValidCSRFToken_ExecuteAdministrationTarget_should401Unauthorized(String path)
    throws Exception {
    mockMvc.perform(post("/action/" + path)
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({"data/taskParameters, fill", "maintain/DelSqlCol, replace-columns"})
  @WithMockUser(username = "test", roles = {"ADMIN", "USER"})
  @DisplayName("An administration target of the modules runs for a signed-in administrator")
  void givenAdministratorAndValidCSRFToken_ExecuteAdministrationTarget_should200Ok(String path, String action)
    throws Exception {
    mockMvc.perform(post("/action/" + path)
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[*].type", hasItem(action)));
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf().useInvalidToken())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser()
  void givenAuthUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf().useInvalidToken())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isForbidden());
  }

  @Test
  void givenInvalidJWTToken_shouldUnauthorized401() throws Exception {
    mockMvc.perform(post("/api/data/SimpleGetAll")
        .header("Authorization", "invalid-token")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void givenNullJWTToken_shouldUnauthorized401() throws Exception {
    mockMvc.perform(post("/api/data/SimpleGetAll")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }
}
