package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * A client can only launch a maintain target or a query when the session user can open a menu option whose screen
 * uses it (issue #914). The operator restriction hides the tools options, so the targets of the administration
 * screens are refused, while the administrator restriction allows them.
 */
@Tag("integration")
@DisplayName("Screen access enforced tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
@TestPropertySource(properties = {"awe.security.screen-access=enforce", "awe.security.default-restriction=operator"})
class ScreenAccessEnforcedTest extends ScreenAccessSupport {

  @ParameterizedTest(name = "{0} through /action/maintain")
  @ValueSource(strings = {"UsrUnl", "reset2fa", "ProUpd", "DbsUpd", "AweKeyUpd"})
  @DisplayName("An operator cannot launch the maintain of an administration screen through /action/maintain")
  void operatorActionMaintainIsRefused(String target) throws Exception {
    assertThat(actionMaintain(target, "operator")).contains(REFUSED);
  }

  @ParameterizedTest(name = "{0} through /action/maintain")
  @ValueSource(strings = {"UsrUnl", "reset2fa", "ProUpd", "DbsUpd", "AweKeyUpd"})
  @DisplayName("An administrator is not refused the maintain of an administration screen")
  void administratorActionMaintainIsAllowed(String target) throws Exception {
    assertThat(actionMaintain(target, "administrator")).doesNotContain(REFUSED);
  }

  @ParameterizedTest(name = "{0} through /api/maintain")
  @ValueSource(strings = {"UsrUnl", "reset2fa", "ProUpd", "DbsUpd", "AweKeyUpd"})
  @DisplayName("A user restricted by default cannot launch the maintain of an administration screen through /api")
  void apiMaintainIsRefused(String target) throws Exception {
    String response = mockMvc.perform(post("/api/maintain/" + target)
        .header("Authorization", "Bearer " + token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"parameters\":{}}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertThat(response).contains(REFUSED);
  }

  @Test
  @DisplayName("An operator cannot launch the quick-filter query of an administration screen through /action/data")
  void operatorActionQueryIsRefused() throws Exception {
    assertThat(actionData("UsrStaCnt", "operator")).contains(REFUSED);
  }

  @Test
  @DisplayName("An administrator can launch the quick-filter query of an administration screen")
  void administratorActionQueryIsAllowed() throws Exception {
    assertThat(actionData("UsrStaCnt", "administrator")).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A user restricted by default cannot launch the quick-filter query of an administration screen through /api")
  void apiQueryIsRefused() throws Exception {
    String response = mockMvc.perform(post("/api/data/UsrStaCnt")
        .header("Authorization", "Bearer " + token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"parameters\":{}}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertThat(response).contains(REFUSED);
  }

  @Test
  @DisplayName("An administrator can sort the grid of a screen by a column the restrictions do not have")
  void administratorCanSortAGridByAnyColumn() throws Exception {
    String sorted = "{\"max\":30,\"page\":1,\"sort\":[{\"id\":\"TypIco\",\"direction\":\"asc\"}]}";

    assertThat(actionData("SecAudEvtLst", "administrator", sorted)).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("An operator can still launch the targets of the screens it can open")
  void operatorCanLaunchTargetsOfAvailableScreens() throws Exception {
    assertThat(actionData("ModNamByOpeSel", "operator")).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A target that no screen uses is not checked")
  void unindexedTargetIsNotChecked() throws Exception {
    assertThat(actionData("getScreenRestrictions", "operator")).doesNotContain(REFUSED);
  }
}
