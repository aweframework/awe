package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * In warn mode a launch without access to a screen that uses the target is logged and audited, but not refused
 */
@Tag("integration")
@DisplayName("Screen access warn tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
@TestPropertySource(properties = {"awe.security.authorization.mode=warn", "awe.security.default-restriction=operator"})
class ScreenAccessWarnTest extends ScreenAccessSupport {

  @Test
  @DisplayName("Warn mode does not refuse the maintain of an administration screen to an operator")
  void operatorMaintainIsNotRefused() throws Exception {
    assertThat(actionMaintain("UsrUnl", "operator")).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("Warn mode does not refuse the quick-filter query of an administration screen to an operator")
  void operatorQueryIsNotRefused() throws Exception {
    assertThat(actionData("UsrStaCnt", "operator")).doesNotContain(REFUSED);
  }
}
