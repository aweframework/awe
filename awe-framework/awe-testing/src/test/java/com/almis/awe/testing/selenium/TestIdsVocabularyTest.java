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
 * Keeps the Java vocabulary ({@link TestIds}, {@link TestAttributes}) in sync with the ones the AngularJS client
 * ({@code awe-client-angular/src/main/resources/js/awe/data/testIds.js}) and the React client
 * ({@code awe-client-react/src/utilities/testIds.js}) render, so they cannot drift silently. A Java constant must exist in
 * at least one client and, wherever it exists, with the same value.
 *
 * <p>The JavaScript files are read from the repository. When the module is built standalone and a file is not
 * reachable, the tests are skipped with an explicit message instead of passing silently.</p>
 */
class TestIdsVocabularyTest {

  static final String ANGULAR_VOCABULARY_FILE = "awe-client-angular/src/main/resources/js/awe/data/testIds.js";
  static final String REACT_VOCABULARY_FILE = "awe-client-react/src/utilities/testIds.js";
  private static final Pattern ENTRY = Pattern.compile("(\\w+)\\s*:\\s*\"([^\"]*)\"");

  @Test
  void shouldKeepTheValuesOfTheJavaVocabularyInSyncWithTheAngularJsOne() throws IOException {
    assertValuesInSync(ANGULAR_VOCABULARY_FILE);
  }

  @Test
  void shouldKeepTheValuesOfTheJavaVocabularyInSyncWithTheReactOne() throws IOException {
    assertValuesInSync(REACT_VOCABULARY_FILE);
  }

  @Test
  void shouldDeclareEveryJavaTestIdAndTestAttributeInAtLeastOneClient() throws IOException {
    for (String constant : List.of("TestIds", "TestAttributes")) {
      Map<String, String> java = readJavaConstants(constant.equals("TestIds") ? TestIds.class : TestAttributes.class);
      Map<String, String> angular = readJavaScriptConstant(ANGULAR_VOCABULARY_FILE, constant);
      Map<String, String> react = readJavaScriptConstant(REACT_VOCABULARY_FILE, constant);

      java.keySet().forEach(name -> assertThat(angular.containsKey(toCamelCase(name)) || react.containsKey(toCamelCase(name)))
        .as("%s.%s must exist in the AngularJS or the React vocabulary", constant, name).isTrue());
    }
  }

  @Test
  void shouldDeclareInTheReactVocabularyTheHooksOnlyReactRenders() throws IOException {
    Map<String, String> react = readJavaScriptConstant(REACT_VOCABULARY_FILE, "TestIds");

    assertThat(react).containsEntry("avatar", TestIds.AVATAR)
      .containsEntry("avatarName", TestIds.AVATAR_NAME)
      .containsEntry("criterionUnit", TestIds.CRITERION_UNIT)
      .containsEntry("gridRowEdit", TestIds.GRID_ROW_EDIT)
      .containsEntry("wizardStepNumber", TestIds.WIZARD_STEP_NUMBER)
      .containsEntry("loadingSpinner", TestIds.LOADING_SPINNER);
  }

  @Test
  void shouldRenderHooksAsSelectors() {
    assertThat(TestIds.css(TestIds.GRID_ROW)).isEqualTo("[data-testid='grid-row']");
    assertThat(TestIds.xpath(TestIds.GRID_ROW)).isEqualTo("@data-testid='grid-row'");
    assertThat(TestAttributes.css(TestAttributes.SELECTED, true)).isEqualTo("[data-selected='true']");
    assertThat(TestAttributes.xpath(TestAttributes.OPEN, true)).isEqualTo("@data-open='true'");
  }

  /**
   * Check that the Java constants declared by a client vocabulary have the same value as in that vocabulary. Only that
   * file is read, so a missing file of the other client does not skip the check
   *
   * @param vocabularyFile Path of the client vocabulary, relative to the {@code awe-framework} directory
   */
  private static void assertValuesInSync(String vocabularyFile) throws IOException {
    int compared = 0;
    for (String constant : List.of("TestIds", "TestAttributes")) {
      Map<String, String> java = readJavaConstants(constant.equals("TestIds") ? TestIds.class : TestAttributes.class);
      Map<String, String> client = readJavaScriptConstant(vocabularyFile, constant);

      for (Map.Entry<String, String> entry : java.entrySet()) {
        String key = toCamelCase(entry.getKey());
        if (client.containsKey(key)) {
          assertThat(client).as("%s.%s must have the value of %s", constant, entry.getKey(), vocabularyFile)
            .containsEntry(key, entry.getValue());
          compared++;
        }
      }
    }
    assertThat(compared).as("Java constants found in %s", vocabularyFile).isPositive();
  }

  /**
   * Read the string entries of an exported JavaScript object literal
   *
   * @param vocabularyFile Path of the file, relative to the {@code awe-framework} directory
   * @param name           Name of the exported constant
   * @return Entries by key
   */
  static Map<String, String> readJavaScriptConstant(String vocabularyFile, String name) throws IOException {
    Path file = locateVocabulary(vocabularyFile);
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
   * Locate a JavaScript vocabulary from the module directory (Maven) or the repository root (IDE)
   */
  private static Path locateVocabulary(String vocabularyFile) {
    Path base = Path.of(System.getProperty("basedir", System.getProperty("user.dir"))).toAbsolutePath();
    List<Path> candidates = List.of(
      base.resolve("..").resolve(vocabularyFile),
      base.resolve("awe-framework").resolve(vocabularyFile));
    Path found = candidates.stream().map(Path::normalize).filter(Files::isRegularFile).findFirst().orElse(null);
    assumeTrue(found != null, "Assumption: the client modules are next to awe-testing, but "
      + vocabularyFile + " was not found from " + base + " (module built standalone?). Vocabulary sync not checked");
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
