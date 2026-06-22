package com.almis.awe.service.totp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-level regression test for the {@code force-qr-code} query descriptor boundary.
 *
 * <p>Background: FORCE-mode 2FA enrollment sets {@code pendingTotpEnrollment=true} on the
 * principal, which causes {@code AweSession.isAuthenticated()} to return {@code false}.
 * The {@code activate-2fa} screen autoloads {@code force-qr-code} during this pre-enrollment
 * window. Without {@code public="true"} on that query,
 * {@code QueryService.getQuery(id, checkSession=true)} throws a
 * {@code SessionAuthenticationException} before the service can run.
 *
 * <p>This test proves the exact XML attribute boundary that was fixed:
 * removing {@code public="true"} from the {@code force-qr-code} descriptor would
 * cause this test to fail immediately, restoring the regression.
 */
class ForceQrCodeDescriptorBoundaryTest {

  private static final Path MODULE_ROOT = Path.of("");
  private static final Path QUERIES_XML = MODULE_ROOT
    .resolve("../awe-generic-screens/src/main/resources/application/awe/global/Queries.xml")
    .normalize();

  /**
   * {@code force-qr-code} must carry {@code public="true"} so that
   * {@code QueryService.getQuery(id, checkSession=true)} bypasses the session check
   * for pending-enrollment users (where {@code AweSession.isAuthenticated()} is false).
   *
   * <p>Regression: removing {@code public="true"} here is the exact change that broke
   * the FORCE enrollment bootstrap path.
   */
  @Test
  void forceQrCodeQuery_mustBePublic_soActivate2faAutoloadWorksBeforeEnrollment() throws IOException {
    String queries = Files.readString(QUERIES_XML);

    // Locate the force-qr-code query declaration and assert public="true" is present
    int forceQrCodeIndex = queries.indexOf("id=\"force-qr-code\"");
    assertThat(forceQrCodeIndex)
      .as("force-qr-code query must exist in Queries.xml")
      .isGreaterThanOrEqualTo(0);

    // Extract a narrow window around the declaration to assert the attribute
    int declarationEnd = queries.indexOf("/>", forceQrCodeIndex);
    if (declarationEnd < 0) {
      declarationEnd = queries.indexOf(">", forceQrCodeIndex);
    }
    String declaration = queries.substring(forceQrCodeIndex, declarationEnd + 1);

    assertThat(declaration)
      .as("force-qr-code must have public=\"true\" — removing it breaks the FORCE enrollment bootstrap")
      .contains("public=\"true\"");
  }

  /**
   * The private {@code qr-code} query (used by fully-authenticated settings flow) must
   * NOT carry {@code public="true"}. This proves the public flag is intentionally
   * limited to the FORCE enrollment path and not accidentally applied to all QR endpoints.
   */
  @Test
  void privateQrCodeQuery_mustNotBePublic_onlyForceVariantIsPublic() throws IOException {
    String queries = Files.readString(QUERIES_XML);

    // Find the private qr-code query (id="qr-code" without the "force-" prefix)
    int qrCodeIndex = queries.indexOf("id=\"qr-code\"");
    assertThat(qrCodeIndex)
      .as("qr-code query must exist in Queries.xml")
      .isGreaterThanOrEqualTo(0);

    int declarationEnd = queries.indexOf("/>", qrCodeIndex);
    if (declarationEnd < 0) {
      declarationEnd = queries.indexOf(">", qrCodeIndex);
    }
    String declaration = queries.substring(qrCodeIndex, declarationEnd + 1);

    assertThat(declaration)
      .as("qr-code (private variant) must NOT be public — only force-qr-code carries public=\"true\"")
      .doesNotContain("public=\"true\"");
  }

  /**
   * Both {@code force-qr-code} and {@code qr-code} must delegate to the same
   * {@code qr-code} service. A typo in the service reference would silently break
   * the enrollment bootstrap without failing any other test.
   */
  @Test
  void forceQrCodeQuery_mustDelegateToQrCodeService() throws IOException {
    String queries = Files.readString(QUERIES_XML);

    int forceQrCodeIndex = queries.indexOf("id=\"force-qr-code\"");
    assertThat(forceQrCodeIndex)
      .as("force-qr-code query must exist in Queries.xml")
      .isGreaterThanOrEqualTo(0);

    int declarationEnd = queries.indexOf("/>", forceQrCodeIndex);
    if (declarationEnd < 0) {
      declarationEnd = queries.indexOf(">", forceQrCodeIndex);
    }
    String declaration = queries.substring(forceQrCodeIndex, declarationEnd + 1);

    assertThat(declaration)
      .as("force-qr-code must delegate to service=\"qr-code\" — a typo would silently break enrollment")
      .contains("service=\"qr-code\"");
  }
}
