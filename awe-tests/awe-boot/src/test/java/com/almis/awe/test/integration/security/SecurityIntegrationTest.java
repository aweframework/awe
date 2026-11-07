package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("Security Tests")
class SecurityIntegrationTest extends AbstractSpringAppIntegrationTest {

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
      .andExpect(status().isUnauthorized());
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
