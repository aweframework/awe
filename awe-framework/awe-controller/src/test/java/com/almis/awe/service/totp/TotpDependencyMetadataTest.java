package com.almis.awe.service.totp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TotpDependencyMetadataTest {

  private static final Path MODULE_ROOT = Path.of("");

  @Test
  void dependencyManagementControlsOtpJavaAndCommonsCodecWithoutSamStevens() throws IOException {
    String dependencyManagementPom = Files.readString(MODULE_ROOT.resolve("../awe-dependencies/pom.xml").normalize());

    assertThat(dependencyManagementPom)
      .containsPattern("<otp-java\\.version>\\d+\\.\\d+\\.\\d+</otp-java\\.version>")
      .containsPattern("<commons-codec\\.version>\\d+\\.\\d+(\\.\\d+)?</commons-codec\\.version>")
      .containsPattern("(?s)<artifactId>otp-java</artifactId>\\s*<version>\\$\\{otp-java\\.version}</version>")
      .containsPattern("(?s)<artifactId>commons-codec</artifactId>\\s*<version>\\$\\{commons-codec\\.version}</version>")
      .doesNotContain("samstevens");
  }

  @Test
  void modulePomsOnlyReferenceAweOwnedTotpDependencies() throws IOException {
    String controllerPom = Files.readString(MODULE_ROOT.resolve("pom.xml"));
    String starterPom = Files.readString(MODULE_ROOT.resolve("../awe-starters/awe-spring-boot-starter/pom.xml").normalize());

    assertThat(controllerPom)
      .contains("<artifactId>otp-java</artifactId>")
      .contains("<artifactId>commons-codec</artifactId>")
      .doesNotContain("samstevens");

    assertThat(starterPom)
      .contains("<artifactId>awe-controller</artifactId>")
      .doesNotContain("samstevens");
  }

  @Test
  void totpCodePathsDoNotKeepStaleLegacyComments() throws IOException {
    String totpService = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/service/TotpService.java"));
    String totpController = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/controller/TotpController.java"));

    assertThat(totpService)
      .doesNotContain("Retrieve QR Code in PNG format as String")
      .doesNotContain("Generate QR code");

    assertThat(totpController)
      .doesNotContain("Handler for index page")
      .doesNotContain("Index page");
  }

  @Test
  void totpCodePathsDoNotReferenceLegacySamStevensTypes() throws IOException {
    String totpService = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/service/TotpService.java"));
    String totpController = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/controller/TotpController.java"));
    String totpServiceTest = Files.readString(MODULE_ROOT.resolve("src/test/java/com/almis/awe/service/TotpServiceTest.java"));

    assertThat(totpService)
      .doesNotContain("TotpAutoConfiguration")
      .doesNotContain("QrGenerationException");

    assertThat(totpController)
      .doesNotContain("TotpAutoConfiguration")
      .doesNotContain("QrGenerationException");

    assertThat(totpServiceTest)
      .doesNotContain("TotpAutoConfiguration")
      .doesNotContain("QrGenerationException");
  }

  @Test
  void productionTotpBoundaryDoesNotExposeTestOnlyCodeGenerationApi() throws IOException {
    String operationsInterface = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/service/totp/AweTotpOperations.java"));
    String operationsImplementation = Files.readString(MODULE_ROOT.resolve("src/main/java/com/almis/awe/service/totp/OtpJavaTotpOperations.java"));

    assertThat(operationsInterface).doesNotContain("generateCode(");
    assertThat(operationsImplementation).doesNotContain("generateCode(");
  }
}
