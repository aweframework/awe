package com.almis.awe.testing.selenium;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guard for the locator literals that {@code SeleniumUtilities} builds itself ({@code By.cssSelector("...")},
 * {@code By.xpath("...")}, {@code By.id("...")}): they cannot depend on library internals either.
 *
 * <p>Not a locator, and therefore out of scope: the {@code fa-mouse-pointer} icon that {@code showMouse} injects in the
 * page, which is a script and never used to find an element.</p>
 */
class SeleniumUtilitiesLiteralsGuardTest {

  private static final Path SOURCE = Path.of("src/main/java/com/almis/awe/testing/utilities/SeleniumUtilities.java");
  private static final Pattern LOCATOR_LITERAL = Pattern.compile("By\\.(?:cssSelector|xpath|id|className)\\(\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

  @Test
  void shouldNotDependOnLibraryInternalsInLocatorLiterals() throws IOException {
    Path source = Path.of(System.getProperty("basedir", System.getProperty("user.dir"))).resolve(SOURCE);
    assumeTrue(Files.isRegularFile(source), "Assumption: the sources of awe-testing are available at " + source);

    Matcher literal = LOCATOR_LITERAL.matcher(Files.readString(source, StandardCharsets.UTF_8));
    List<String> literals = new ArrayList<>();
    List<String> violations = new ArrayList<>();
    while (literal.find()) {
      literals.add(literal.group(1));
      AbstractFrontEndSelectorGuardTest.findForbiddenTokens(literal.group(0), AbstractFrontEndSelectorGuardTest.defaultForbiddenTokens())
        .forEach(token -> violations.add(literal.group(0) + "   [forbidden: " + token.name() + "]"));
    }

    assertThat(literals).as("The guard must find the locator literals of SeleniumUtilities").isNotEmpty();
    assertThat(violations).as("Locator literals that depend on library internals").isEmpty();
  }
}
