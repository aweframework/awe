package com.almis.awe.testing.selenium;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Keeps the Java vocabulary ({@link TestIds}, {@link TestAttributes}) in sync with the one the AngularJS client renders
 * ({@code awe-client-angular/src/main/resources/js/awe/data/testIds.js}), so the two cannot drift silently.
 *
 * <p>The JavaScript file is read from the repository. When the module is built standalone and the file is not
 * reachable, the tests are skipped with an explicit message instead of passing silently.</p>
 */
class TestIdsVocabularyTest {

  private static final String VOCABULARY_FILE = "awe-client-angular/src/main/resources/js/awe/data/testIds.js";
  private static final Pattern ENTRY = Pattern.compile("(\\w+)\\s*:\\s*\"([^\"]*)\"");

  @Test
  void shouldDeclareEveryJavaTestIdWithTheSameValueAsTheJavaScriptVocabulary() throws IOException {
    Map<String, String> javaScript = readJavaScriptConstant("TestIds");

    Map<String, String> java = readJavaConstants(TestIds.class);

    assertThat(java).isNotEmpty();
    java.forEach((name, value) -> assertThat(javaScript)
      .as("TestIds.%s must exist in testIds.js", name)
      .containsEntry(toCamelCase(name), value));
  }

  @Test
  void shouldDeclareEveryJavaTestAttributeWithTheSameNameAsTheJavaScriptVocabulary() throws IOException {
    Map<String, String> javaScript = readJavaScriptConstant("TestAttributes");

    Map<String, String> java = readJavaConstants(TestAttributes.class);

    assertThat(java).isNotEmpty();
    java.forEach((name, value) -> assertThat(javaScript)
      .as("TestAttributes.%s must exist in testIds.js", name)
      .containsEntry(toCamelCase(name), value));
  }

  @Test
  void shouldRenderHooksAsSelectors() {
    assertThat(TestIds.css(TestIds.GRID_ROW)).isEqualTo("[data-testid='grid-row']");
    assertThat(TestIds.xpath(TestIds.GRID_ROW)).isEqualTo("@data-testid='grid-row'");
    assertThat(TestAttributes.css(TestAttributes.SELECTED, true)).isEqualTo("[data-selected='true']");
    assertThat(TestAttributes.xpath(TestAttributes.OPEN, true)).isEqualTo("@data-open='true'");
  }

  /**
   * Read the string entries of an exported JavaScript object literal
   */
  private static Map<String, String> readJavaScriptConstant(String name) throws IOException {
    Path file = locateVocabulary();
    String source = Files.readString(file, StandardCharsets.UTF_8)
      .replaceAll("(?s)/\\*.*?\\*/", "")
      .replaceAll("(?m)^\\s*//.*$", "");

    Matcher block = Pattern.compile("export const " + name + " = Object\\.freeze\\(\\{(.*?)}\\);", Pattern.DOTALL)
      .matcher(source);
    assertThat(block.find()).as("%s must be exported in %s", name, file).isTrue();

    Map<String, String> entries = new LinkedHashMap<>();
    Matcher entry = ENTRY.matcher(block.group(1));
    while (entry.find()) {
      entries.put(entry.group(1), entry.group(2));
    }
    return entries;
  }

  /**
   * Locate the JavaScript vocabulary from the module directory (Maven) or the repository root (IDE)
   */
  private static Path locateVocabulary() {
    Path base = Path.of(System.getProperty("basedir", System.getProperty("user.dir"))).toAbsolutePath();
    List<Path> candidates = List.of(
      base.resolve("..").resolve(VOCABULARY_FILE),
      base.resolve("awe-framework").resolve(VOCABULARY_FILE));
    Path found = candidates.stream().map(Path::normalize).filter(Files::isRegularFile).findFirst().orElse(null);
    assumeTrue(found != null, "Assumption: the awe-client-angular module is next to awe-testing, but "
      + VOCABULARY_FILE + " was not found from " + base + " (module built standalone?). Vocabulary sync not checked");
    return found;
  }

  private static Map<String, String> readJavaConstants(Class<?> type) {
    return Arrays.stream(type.getDeclaredFields())
      .filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()))
      .filter(field -> String.class.equals(field.getType()))
      .collect(Collectors.toMap(Field::getName, TestIdsVocabularyTest::valueOf, (a, b) -> a, LinkedHashMap::new));
  }

  private static String valueOf(Field field) {
    try {
      return (String) field.get(null);
    } catch (IllegalAccessException exc) {
      throw new IllegalStateException(exc);
    }
  }

  private static String toCamelCase(String constantName) {
    StringBuilder camel = new StringBuilder();
    boolean upper = false;
    for (char character : constantName.toLowerCase().toCharArray()) {
      if (character == '_') {
        upper = true;
      } else {
        camel.append(upper ? Character.toUpperCase(character) : character);
        upper = false;
      }
    }
    return camel.toString();
  }
}
