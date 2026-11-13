package com.almis.awe.testing.guard;

import com.almis.awe.testing.annotations.DependentChain;
import com.almis.awe.testing.annotations.Quarantine;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests of the guard that keeps the declarations of quarantined tests and dependent chains honest
 */
class BrowserTestDeclarationsGuardTest {

  static class Plain {
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @Quarantine(issue = "#766", reason = "The suggest sometimes answers late")
  static class QuarantinedClassWithIssueNumber {
  }

  static class QuarantinedMethodWithIssueUrl {
    @Quarantine(issue = "https://gitlab.com/aweframework/awe/-/issues/766", reason = "Late suggest")
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @Quarantine(issue = "flaky", reason = "Late suggest")
  static class QuarantinedWithoutIssue {
  }

  @Quarantine(issue = "#766", reason = " ")
  static class QuarantinedWithoutReason {
  }

  @Tag("quarantine")
  static class TaggedByHand {
  }

  static class MethodTaggedByHand {
    @Tag("quarantine")
    void test() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @TestMethodOrder(MethodOrderer.MethodName.class)
  @DependentChain("The update needs the record that the create made")
  static class DeclaredChain {
    void t000_create() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @TestMethodOrder(MethodOrderer.MethodName.class)
  @DependentChain("The update needs the record that the create made")
  @Quarantine(issue = "#766", reason = "The whole chain is flaky")
  static class QuarantinedChain {
  }

  @DependentChain("The update needs the record that the create made")
  static class ChainWithoutOrder {
  }

  @TestMethodOrder(MethodOrderer.MethodName.class)
  @DependentChain(" ")
  static class ChainWithoutReason {
  }

  @Tag("dependent-chain")
  static class ChainTaggedByHand {
  }

  @TestMethodOrder(MethodOrderer.MethodName.class)
  @DependentChain("The update needs the record that the create made")
  static class ChainWithAQuarantinedStep {
    @Quarantine(issue = "#766", reason = "Late suggest")
    void t020_update() {
      // Fixture of the test: only its annotations matter, so the method is empty on purpose
    }
  }

  @Test
  void shouldAcceptClassesWithoutDeclarations() {
    assertThat(BrowserTestDeclarationsGuard.violations(Plain.class)).isEmpty();
  }

  @Test
  void shouldAcceptAQuarantineWithAnIssueNumberOrAnIssueUrlAndAReason() {
    assertThat(BrowserTestDeclarationsGuard.violations(QuarantinedClassWithIssueNumber.class,
      QuarantinedMethodWithIssueUrl.class)).isEmpty();
  }

  @Test
  void shouldRejectAQuarantineWithoutAnIssueReference() {
    List<String> violations = BrowserTestDeclarationsGuard.violations(QuarantinedWithoutIssue.class);

    assertThat(violations).singleElement().asString()
      .contains("QuarantinedWithoutIssue").contains("\"flaky\"").contains("issue");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "766", "issue 766", "#", "http://example.com/page"})
  void shouldRejectTextThatIsNotAnIssueReference(String issue) {
    assertThat(BrowserTestDeclarationsGuard.isIssueReference(issue)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"#1", "#766", "https://gitlab.com/aweframework/awe/-/issues/766", "http://git.local/group/p/issues/12"})
  void shouldAcceptAnIssueNumberOrAnIssueUrl(String issue) {
    assertThat(BrowserTestDeclarationsGuard.isIssueReference(issue)).isTrue();
  }

  @Test
  void shouldRejectAQuarantineWithoutReason() {
    assertThat(BrowserTestDeclarationsGuard.violations(QuarantinedWithoutReason.class)).singleElement().asString()
      .contains("QuarantinedWithoutReason").contains("reason");
  }

  @Test
  void shouldRejectTheQuarantineTagGivenByHand() {
    assertThat(BrowserTestDeclarationsGuard.violations(TaggedByHand.class)).singleElement().asString()
      .contains("TaggedByHand").contains("@Quarantine");
    assertThat(BrowserTestDeclarationsGuard.violations(MethodTaggedByHand.class)).singleElement().asString()
      .contains("MethodTaggedByHand#test").contains("@Quarantine");
  }

  @Test
  void shouldAcceptAChainThatDeclaresItsOrderAndItsReason() {
    assertThat(BrowserTestDeclarationsGuard.violations(DeclaredChain.class, QuarantinedChain.class)).isEmpty();
  }

  @Test
  void shouldRejectAChainWithoutOrder() {
    assertThat(BrowserTestDeclarationsGuard.violations(ChainWithoutOrder.class)).singleElement().asString()
      .contains("ChainWithoutOrder").contains("@TestMethodOrder");
  }

  @Test
  void shouldRejectAChainWithoutReason() {
    assertThat(BrowserTestDeclarationsGuard.violations(ChainWithoutReason.class)).singleElement().asString()
      .contains("ChainWithoutReason").contains("reason");
  }

  @Test
  void shouldRejectTheChainTagGivenByHand() {
    assertThat(BrowserTestDeclarationsGuard.violations(ChainTaggedByHand.class)).singleElement().asString()
      .contains("ChainTaggedByHand").contains("@DependentChain");
  }

  @Test
  void shouldRejectTheQuarantineOfOneStepOfAChain() {
    assertThat(BrowserTestDeclarationsGuard.violations(ChainWithAQuarantinedStep.class)).singleElement().asString()
      .contains("ChainWithAQuarantinedStep#t020_update").contains("whole class");
  }

  @Test
  void shouldScanTheITClassesOfAPackage() throws IOException, URISyntaxException {
    Path classes = Path.of(Plain.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    List<String> violations = BrowserTestDeclarationsGuard.scan(classes, "com.almis.awe.testing.guard.fixtures");

    assertThat(violations).singleElement().asString().contains("BrokenQuarantineIT").contains("issue");
  }

  @Test
  void shouldFailToScanAPackageThatDoesNotExist() throws URISyntaxException {
    Path classes = Path.of(Plain.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    assertThatThrownBy(() -> BrowserTestDeclarationsGuard.scan(classes, "com.almis.missing"))
      .isInstanceOf(IOException.class).hasMessageContaining("com.almis.missing");
  }
}
