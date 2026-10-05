package com.almis.awe.testing.utilities;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Keeps the Selenium test guide ({@code website/docs/guides/selenium-test-guide.md}) in sync with the steps that
 * {@link SeleniumUtilities} offers to test writers, so a new step cannot be added without being documented.
 *
 * <p>Rule: every method declared by {@code SeleniumUtilities} that is {@code public} or {@code protected}, is not
 * {@code @Deprecated} and is not synthetic is a step, and the guide must mention it as {@code name(}. Deprecated steps
 * are exempt (the guide lists them in the deprecation table, but they may disappear). The infrastructure methods
 * {@link #INFRASTRUCTURE} (driver, base URL and model wiring) are not steps and are exempt. Overloads share one
 * mention, because the guide lists them together under the name.</p>
 *
 * <p>The guide is read from the repository. When the module is built standalone and the file is not reachable, the
 * test is skipped with an explicit message instead of failing.</p>
 */
class SeleniumTestGuideSyncTest {

  private static final String GUIDE_FILE = "website/docs/guides/selenium-test-guide.md";
  /**
   * Methods of the façade that wire the driver and the model; test writers do not call them as steps
   */
  private static final Set<String> INFRASTRUCTURE = Set.of("getDriver", "getBaseUrl", "setSeleniumModel");

  @Test
  void shouldDocumentEveryPublicOrProtectedStepInTheSeleniumTestGuide() throws IOException {
    Path guide = locateGuide();
    assumeTrue(guide != null, GUIDE_FILE + " is not reachable from this module: guide sync check skipped");
    String content = Files.readString(guide, StandardCharsets.UTF_8);

    Set<String> undocumented = new TreeSet<>();
    for (String step : steps()) {
      if (!Pattern.compile("\\b" + Pattern.quote(step) + "\\(").matcher(content).find()) {
        undocumented.add(step);
      }
    }

    assertThat(undocumented)
      .as("Steps of SeleniumUtilities that %s does not mention as 'name(' (add them to the step catalogue)", GUIDE_FILE)
      .isEmpty();
  }

  @Test
  void shouldFindTheStepsOfTheFacade() {
    assertThat(steps()).contains("clickButton", "checkLogin", "toggleRowContents").doesNotContainAnyElementsOf(INFRASTRUCTURE);
  }

  private static List<String> steps() {
    return Arrays.stream(SeleniumUtilities.class.getDeclaredMethods())
      .filter(method -> !method.isSynthetic())
      .filter(method -> Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()))
      .filter(method -> !method.isAnnotationPresent(Deprecated.class))
      .map(Method::getName)
      .filter(name -> !INFRASTRUCTURE.contains(name))
      .distinct()
      .sorted()
      .toList();
  }

  private static Path locateGuide() {
    Path base = Path.of(System.getProperty("basedir", System.getProperty("user.dir"))).toAbsolutePath();
    return List.of(base.resolve("..").resolve("..").resolve(GUIDE_FILE), base.resolve(GUIDE_FILE)).stream()
      .filter(Files::isRegularFile)
      .findFirst()
      .orElse(null);
  }
}
