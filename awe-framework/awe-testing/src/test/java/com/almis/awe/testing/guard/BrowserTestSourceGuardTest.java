package com.almis.awe.testing.guard;

import com.almis.awe.testing.guard.BrowserTestSourceGuard.Report;
import com.almis.awe.testing.guard.BrowserTestSourceGuard.Violation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests of the guard that keeps browser test sources free of selectors and automation-tool types
 */
class BrowserTestSourceGuardTest {

  @TempDir
  Path directory;

  private Report scanBody(String body) throws IOException {
    return scanBody(BrowserTestSourceGuard.create(), body);
  }

  private Report scanBody(BrowserTestSourceGuard guard, String body) throws IOException {
    write("SampleIT.java", "package sample;\n\nclass SampleIT extends Base {\n  void step() {\n" + body + "\n  }\n}\n");
    return guard.scan(directory);
  }

  private void write(String relativePath, String content) throws IOException {
    Path file = directory.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  @Test
  void shouldAcceptAStepsOnlyClass() throws IOException {
    Report report = scanBody("""
      clickButton("button-ok");
      checkText(getInfoButton(), "Done");
      checkLogin("test", "test", "Welcome: Test user");
      checkLogout();
      """);

    assertThat(report.violations()).isEmpty();
    assertThat(report.isClean()).isTrue();
  }

  @Test
  void shouldFlagSeleniumImports() throws IOException {
    write("SampleIT.java", """
      package sample;

      import org.openqa.selenium.By;
      import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
      import java.util.List;

      class SampleIT {
      }
      """);

    List<Violation> violations = BrowserTestSourceGuard.create().scan(directory).violations();

    assertThat(violations).extracting(Violation::rule, Violation::line)
      .containsExactly(org.assertj.core.groups.Tuple.tuple("forbidden-import", 3), org.assertj.core.groups.Tuple.tuple("forbidden-import", 4));
    assertThat(violations.get(0).file()).isEqualTo("SampleIT.java");
    assertThat(violations.get(0).snippet()).isEqualTo("import org.openqa.selenium.By;");
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "driver.findElement(By.cssSelector(\"x\"));",
    "WebDriver wd = getDriver();",
    "WebElement element = null;",
    "((JavascriptExecutor) wd).executeScript(\"return 1\");",
    "new Select(element).selectByIndex(1);",
    "new Actions(wd).perform();",
    "click(TestIds.BUTTON);",
    "String attribute = TestAttributes.TEST_ID;",
    "By selector = null;"
  })
  void shouldFlagAutomationToolTypesAndLocatorVocabulary(String statement) throws IOException {
    Report report = scanBody(statement);

    assertThat(report.violations()).isNotEmpty();
    assertThat(report.violations()).allSatisfy(violation -> {
      assertThat(violation.rule()).startsWith("forbidden-identifier");
      assertThat(violation.line()).isEqualTo(5);
      assertThat(violation.snippet()).isEqualTo(statement);
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "click(\"[data-testid='x']\");",
    "click(\"#identifier\");",
    "click(\"button.primary\");",
    "click(\"div > span\");",
    "click(\"//div[@id='x']\");",
    "clearText(\"input:first-child\");",
    "checkText(\"p.title\", \"Title\");",
    "checkTextContains(\".title\", \"Title\");",
    "checkTextNotContains(\".title\", \"Title\");",
    "checkPresence(\".title\");",
    "checkVisible(\".title\");",
    "checkNotVisible(\".title\");",
    "checkVisibleAndContains(\".title\", \"Title\");",
    "waitForCssSelector(\".title\");",
    "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"Home\");",
    "checkLogin(\"user\", \"password\", \".user-name\", \"Test user\");",
    "checkLogout(\".login-form\", \"Sign in\");"
  })
  void shouldFlagSelectorLiteralsPassedToSelectorHelpers(String statement) throws IOException {
    Report report = scanBody(statement);

    assertThat(report.violations()).hasSize(1);
    Violation violation = report.violations().get(0);
    assertThat(violation.rule()).isEqualTo("selector-literal");
    assertThat(violation.line()).isEqualTo(5);
    assertThat(violation.snippet()).isEqualTo(statement.substring(0, statement.length() - 1));
  }

  @Test
  void shouldFlagAnyLiteralPassedToWaitForTextBecauseItIsACssClass() throws IOException {
    assertThat(scanBody("waitForText(\"alert\", \"Done\");").violations())
      .extracting(Violation::rule).containsExactly("selector-literal");
  }

  @Test
  void shouldFlagTheSelectorOfACallSplitInSeveralLines() throws IOException {
    Report report = scanBody("""
      checkText(
        "div.title",
        "Title");
      """);

    assertThat(report.violations()).hasSize(1);
    assertThat(report.violations().get(0).line()).isEqualTo(5);
    assertThat(report.violations().get(0).snippet()).isEqualTo("checkText( \"div.title\", \"Title\")");
  }

  @Test
  void shouldNotFlagTextsEvenWhenTheyLookLikeSelectors() throws IOException {
    Report report = scanBody("""
      checkText(getInfoButton(), "Version 1.2: [beta] #3 > ok // done");
      checkTextContains(getTitle(), ".html");
      checkLogin("user", "pass.word", "Welcome: Test user.");
      checkVisibleAndContains(getTable(), "a > b");
      click(buttonName);
      writeText(getCriterion("User"), "john.doe@example.com");
      """);

    assertThat(report.violations()).isEmpty();
  }

  @Test
  void shouldIgnoreCommentsAndStringsWhenLookingForTypes() throws IOException {
    Report report = scanBody("""
      // The driver is obtained with getDriver() and By.id("x") is not used any more
      /* WebElement and TestIds.X are gone */
      /**
       * click("#id") was replaced
       */
      String message = "Created By. Actions on WebElement and getDriver() are hidden";
      String block = \"\"\"
        new Select(x) By.id click("#id")
        \"\"\";
      Object action = new MyActions();
      """);

    assertThat(report.violations()).isEmpty();
  }

  @Test
  void shouldHonourAnAllowanceOfAnExactSnippetInAFile() throws IOException {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create()
      .allow("SampleIT.java", "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"Home\")", "Third-party application inside a frame");

    Report report = scanBody(guard, """
      checkTextInEmbeddedFrame("ol.breadcrumb a", "Home");
      checkTextInEmbeddedFrame("ol.other a", "Home");
      """);

    assertThat(report.violations()).extracting(Violation::snippet).containsExactly("checkTextInEmbeddedFrame(\"ol.other a\", \"Home\")");
    assertThat(report.staleAllowances()).isEmpty();
  }

  @Test
  void shouldNotApplyAnAllowanceToAnotherFile() throws IOException {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create()
      .allow("OtherIT.java", "click(\"#id\")", "Other file");

    Report report = scanBody(guard, "click(\"#id\");");

    assertThat(report.violations()).hasSize(1);
    assertThat(report.staleAllowances()).hasSize(1);
  }

  @Test
  void shouldMatchAnAllowanceByTheEndOfTheRelativePath() throws IOException {
    write("deep/er/NestedIT.java", "class NestedIT { void s() { click(\"#id\"); } }");
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create().allow("NestedIT.java", "click(\"#id\")", "Reason");

    assertThat(guard.scan(directory).violations()).isEmpty();
  }

  @Test
  void shouldReportAnAllowanceThatDoesNotMatchAnythingAnymore() throws IOException {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create()
      .allow("SampleIT.java", "click(\"#gone\")", "Was needed once");

    Report report = scanBody(guard, "click(buttonName);");

    assertThat(report.violations()).isEmpty();
    assertThat(report.staleAllowances()).hasSize(1);
    assertThat(report.isClean()).isFalse();
    assertThat(report.describe()).contains("click(\"#gone\")").contains("Was needed once");
  }

  @Test
  void shouldCountOccurrencesWhenAnAllowanceHasATimes() throws IOException {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create().allow("SampleIT.java", "click(\"#id\")", "Pending", 2);

    Report report = scanBody(guard, """
      click("#id");
      click("#id");
      click("#id");
      """);

    assertThat(report.violations()).hasSize(1);
    assertThat(report.violations().get(0).line()).isEqualTo(7);
    assertThat(report.staleAllowances()).isEmpty();
  }

  @Test
  void shouldReportFewerOccurrencesThanAllowed() throws IOException {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create().allow("SampleIT.java", "click(\"#id\")", "Pending", 3);

    Report report = scanBody(guard, "click(\"#id\");");

    assertThat(report.violations()).isEmpty();
    assertThat(report.staleAllowances()).hasSize(1);
    assertThat(report.staleAllowances().get(0).times()).isEqualTo(3);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void shouldRequireAReasonInEveryAllowance(String reason) {
    BrowserTestSourceGuard guard = BrowserTestSourceGuard.create();

    assertThatThrownBy(() -> guard.allow("SampleIT.java", "click(\"#id\")", reason)).isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("reason");
    assertThatThrownBy(() -> guard.allow("SampleIT.java", "click(\"#id\")", null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldWriteAndReadABaselineThatAllowsTheSameViolations() throws IOException {
    write("AIT.java", "class AIT { void s() { click(\"#a\"); click(\"#a\"); WebElement e; } }");
    write("sub/BIT.java", "class BIT { void s() { checkText(\"p.b\", \"x\"); } }");
    String baseline = BrowserTestSourceGuard.create().scan(directory).toBaseline();
    Path baselineFile = directory.resolve("baseline.txt");
    Files.writeString(baselineFile, "# Pending migration\n\n" + baseline);

    assertThat(baseline).contains("AIT.java:click(\"#a\")\nAIT.java:click(\"#a\")\n")
      .contains("sub/BIT.java:checkText(\"p.b\", \"x\")");

    Report report = BrowserTestSourceGuard.create().allowBaseline(baselineFile, "Pending migration").scan(directory);

    assertThat(report.violations()).isEmpty();
    assertThat(report.staleAllowances()).isEmpty();
    assertThat(report.isClean()).isTrue();
  }

  @Test
  void shouldFailARatchetOnNewViolationsAndOnDisappearedOnes() throws IOException {
    write("AIT.java", "class AIT { void s() { click(\"#a\"); } }");
    Path baselineFile = directory.resolve("baseline.txt");
    Files.writeString(baselineFile, BrowserTestSourceGuard.create().scan(directory).toBaseline());

    write("AIT.java", "class AIT { void s() { click(\"#a\"); click(\"#new\"); } }");
    Report withNew = BrowserTestSourceGuard.create().allowBaseline(baselineFile, "Pending").scan(directory);
    assertThat(withNew.violations()).extracting(Violation::snippet).containsExactly("click(\"#new\")");

    write("AIT.java", "class AIT { void s() { } }");
    Report withGone = BrowserTestSourceGuard.create().allowBaseline(baselineFile, "Pending").scan(directory);
    assertThat(withGone.violations()).isEmpty();
    assertThat(withGone.staleAllowances()).hasSize(1);
    assertThat(withGone.describe()).contains("Shrink the baseline").contains("AIT.java").contains("click(\"#a\")");
  }

  @Test
  void shouldRequireABaselineEntryToHaveAFileAndASnippet() throws IOException {
    Path baselineFile = directory.resolve("baseline.txt");
    Files.writeString(baselineFile, "no-separator\n");

    assertThatThrownBy(() -> BrowserTestSourceGuard.create().allowBaseline(baselineFile, "Pending"))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no-separator");
  }

  @Test
  void shouldScanOnlyTheItClassesByDefault() throws IOException {
    write("SampleIT.java", "class SampleIT { void s() { click(\"#id\"); } }");
    write("SampleHelper.java", "class SampleHelper { void s() { click(\"#helper\"); getDriver(); } }");

    List<Violation> violations = BrowserTestSourceGuard.create().scan(directory).violations();

    assertThat(violations).extracting(Violation::file).containsOnly("SampleIT.java");
  }

  @Test
  void shouldScanTheFilesMatchingTheConfiguredGlob() throws IOException {
    write("SampleIT.java", "class SampleIT { void s() { click(\"#id\"); } }");
    write("SampleHelper.java", "class SampleHelper { void s() { click(\"#helper\"); } }");
    write("other/Sample.java", "class Sample { void s() { click(\"#other\"); } }");

    assertThat(BrowserTestSourceGuard.create().files("*Helper.java").scan(directory).violations())
      .extracting(Violation::file).containsExactly("SampleHelper.java");
    assertThat(BrowserTestSourceGuard.create().files("*.java").scan(directory).violations())
      .extracting(Violation::file).containsExactly("SampleHelper.java", "SampleIT.java", "other/Sample.java");
  }

  @Test
  void shouldRequireAGlobToSelectTheFiles() {
    assertThatThrownBy(() -> BrowserTestSourceGuard.create().files(" ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BrowserTestSourceGuard.create().files(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldIgnoreFilesThatAreNotJavaSources() throws IOException {
    write("notes.txt", "click(\"#id\") getDriver()");

    assertThat(BrowserTestSourceGuard.create().scan(directory).violations()).isEmpty();
  }

  @Test
  void shouldFailWhenTheDirectoryDoesNotExist() {
    assertThatThrownBy(() -> BrowserTestSourceGuard.create().scan(directory.resolve("missing")))
      .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("missing");
  }

  @Test
  void shouldDescribeEveryViolationWithFileLineRuleAndSnippet() throws IOException {
    Report report = scanBody("click(\"#id\");");

    assertThat(report.describe()).contains("SampleIT.java:5").contains("selector-literal").contains("click(\"#id\")");
  }
}
