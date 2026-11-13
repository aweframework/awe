package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A client can only launch a maintain target or a query when the session user can open a menu option whose screen
 * uses it (issue #914). The operator restriction hides the tools options, so the targets of the administration
 * screens are refused, while the administrator restriction allows them. The REST API is not checked by the screens
 * ({@link RestApiAuthorizationTest}).
 */
@Tag("integration")
@DisplayName("Screen access enforced tests")
@WithUserDetails(value = "test", userDetailsServiceBeanName = "aweUserDetailsService")
@TestPropertySource(properties = {"awe.security.authorization.mode=enforce", "awe.security.default-restriction=operator"})
class ScreenAccessEnforcedTest extends ScreenAccessSupport {

  @ParameterizedTest(name = "{0} through /action/maintain")
  @ValueSource(strings = {"UsrUnl", "reset2fa", "ProUpd", "DbsUpd", "AweKeyUpd"})
  @DisplayName("An operator cannot launch the maintain of an administration screen through /action/maintain")
  void operatorActionMaintainIsRefused(String target) throws Exception {
    assertRefused(actionMaintain(target, "operator"), target);
  }

  @ParameterizedTest(name = "{0} through /action/maintain")
  @ValueSource(strings = {"UsrUnl", "reset2fa", "ProUpd", "DbsUpd", "AweKeyUpd"})
  @DisplayName("An administrator is not refused the maintain of an administration screen")
  void administratorActionMaintainIsAllowed(String target) throws Exception {
    assertThat(actionMaintain(target, "administrator")).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("An operator cannot launch the quick-filter query of an administration screen through /action/data")
  void operatorActionQueryIsRefused() throws Exception {
    assertRefused(actionData("UsrStaCnt", "operator"), "UsrStaCnt");
  }

  @Test
  @DisplayName("An administrator can launch the quick-filter query of an administration screen")
  void administratorActionQueryIsAllowed() throws Exception {
    assertThat(actionData("UsrStaCnt", "administrator")).doesNotContain(REFUSED);
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
