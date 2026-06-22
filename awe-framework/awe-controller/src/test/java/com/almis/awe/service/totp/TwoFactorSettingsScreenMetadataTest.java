package com.almis.awe.service.totp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorSettingsScreenMetadataTest {

  private static final Path MODULE_ROOT = Path.of("");
  private static final Path ANGULAR_SETTINGS = MODULE_ROOT.resolve("../awe-generic-screens/src/main/resources/application/awe/screen/users/settings.xml").normalize();
  private static final Path REACT_SETTINGS = MODULE_ROOT.resolve("../awe-generic-screens/src/main/resources/application/awe-react/screen/notifications/settings.xml").normalize();

  @Test
  void enable2faFlowGeneratesSecretWithoutCallingUpdateStatusFirst() throws IOException {
    assertThat(enableBranch(Files.readString(ANGULAR_SETTINGS)))
      .contains("target-action=\"generate2faSecret\"")
      .doesNotContain("target-action=\"update2faStatus\"");

    assertThat(enableBranch(Files.readString(REACT_SETTINGS)))
      .contains("target-action=\"generate2faSecret\"")
      .doesNotContain("target-action=\"update2faStatus\"");
  }

  @Test
  void disable2faFlowStillUpdatesStatusWithoutGeneratingSecret() throws IOException {
    assertThat(disableBranch(Files.readString(ANGULAR_SETTINGS)))
      .contains("target-action=\"update2faStatus\"")
      .doesNotContain("target-action=\"generate2faSecret\"");

    assertThat(disableBranch(Files.readString(REACT_SETTINGS)))
      .contains("target-action=\"update2faStatus\"")
      .doesNotContain("target-action=\"generate2faSecret\"");
  }

  private String enableBranch(String xml) {
    return branch(xml, "condition=\"eq\" value=\"1\"", "</dependency>");
  }

  private String disableBranch(String xml) {
    String condition = xml.contains("condition=\"ne\" value=\"1\"")
      ? "condition=\"ne\" value=\"1\""
      : "condition=\"eq\" value=\"0\"";
    return branch(xml, condition, "</dependency>");
  }

  private String branch(String xml, String startMarker, String endMarker) {
    int start = xml.indexOf(startMarker);
    int end = xml.indexOf(endMarker, start);
    return xml.substring(start, end);
  }
}
