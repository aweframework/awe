package com.almis.awe.service.totp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TotpDependencyMetadataTest {

  private static final Path MODULE_ROOT = Path.of("");

  @Test
  void dependencyManagementControlsOtpJavaAndInheritsCommonsCodecWithoutSamStevens() throws IOException {
    String dependencyManagementPom = Files.readString(MODULE_ROOT.resolve("../awe-dependencies/pom.xml").normalize());

    // commons-codec is managed by the Spring Boot BOM; an AWE pin would downgrade it
    assertThat(dependencyManagementPom)
      .containsPattern("<otp-java\\.version>\\d+\\.\\d+\\.\\d+</otp-java\\.version>")
      .containsPattern("(?s)<artifactId>otp-java</artifactId>\\s*<version>\\$\\{otp-java\\.version}</version>")
      .doesNotContain("<commons-codec.version>")
      .doesNotContain("<artifactId>commons-codec</artifactId>")
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
