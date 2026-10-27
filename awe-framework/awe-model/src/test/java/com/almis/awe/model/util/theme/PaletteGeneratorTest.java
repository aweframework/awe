package com.almis.awe.model.util.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The palettes generated from a hex (design system, "Palettes generated from a hex") must be the ones of the design
 * system prototype: theme/generated-palettes.json holds the output of its generator (prototype.js) for the tested hexes
 */
class PaletteGeneratorTest {

  private static final Map<String, Map<String, Map<String, String>>> PROTOTYPE = readPrototypePalettes();

  @SuppressWarnings("unchecked")
  private static Map<String, Map<String, Map<String, String>>> readPrototypePalettes() {
    try (InputStream input = PaletteGeneratorTest.class.getResourceAsStream("/theme/generated-palettes.json")) {
      Map<String, Object> json = new ObjectMapper().readValue(input, new TypeReference<>() {
      });
      return Map.of("primary", (Map<String, Map<String, String>>) json.get("primary"),
        "surface", (Map<String, Map<String, String>>) json.get("surface"));
    } catch (IOException exc) {
      throw new IllegalStateException("The palettes of the prototype could not be read", exc);
    }
  }

  @Test
  void generatesThePrimaryPalettesOfThePrototype() {
    PROTOTYPE.get("primary").forEach((hex, expected) ->
      assertThat(PaletteGenerator.primary(hex)).as("primary %s", hex).containsExactlyEntriesOf(expected));
  }

  @Test
  void generatesTheSurfacePalettesOfThePrototype() {
    PROTOTYPE.get("surface").forEach((hex, expected) ->
      assertThat(PaletteGenerator.surface(hex)).as("surface %s", hex).containsExactlyEntriesOf(expected));
  }

  @Test
  void acceptsShortAndUpperCaseHexes() {
    assertThat(PaletteGenerator.primary("#4f46e5")).isEqualTo(PaletteGenerator.primary("#4F46E5"));
    assertThat(PaletteGenerator.surface("#000")).isEqualTo(PaletteGenerator.surface("#000000"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"#4F46E", "4F46E5", "#GGGGGG", "indigo", ""})
  void recognisesOnlyHexColours(String value) {
    assertThat(PaletteGenerator.isHex(value)).isFalse();
    assertThatThrownBy(() -> PaletteGenerator.primary(value)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PaletteGenerator.surface(value)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recognisesHexColours() {
    assertThat(PaletteGenerator.isHex("#abc")).isTrue();
    assertThat(PaletteGenerator.isHex(" #A1B2C3 ")).isTrue();
    assertThat(PaletteGenerator.isHex(null)).isFalse();
  }
}
