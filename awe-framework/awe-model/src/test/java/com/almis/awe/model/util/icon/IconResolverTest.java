package com.almis.awe.model.util.icon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Icon values of the screen XML resolved with the equivalence table of the clients (#885, S15)
 */
class IconResolverTest {

  @Test
  void resolvesFontAwesomeNamesToTheirLucideEquivalence() {
    assertThat(IconResolver.resolve("home")).contains(new IconResolver.Icon("lucide", "house", ""));
    assertThat(IconResolver.resolve("fa-home")).contains(new IconResolver.Icon("lucide", "house", ""));
    assertThat(IconResolver.resolve("fa:fa-home")).contains(new IconResolver.Icon("lucide", "house", ""));
    assertThat(IconResolver.resolve("fa:sitemap")).contains(new IconResolver.Icon("lucide", "network", ""));
  }

  @Test
  void resolvesMaterialAndPrimeIconsNames() {
    assertThat(IconResolver.resolve("mdi:arrow_back")).contains(new IconResolver.Icon("lucide", "arrow-left", ""));
    assertThat(IconResolver.resolve("mdi:home")).contains(new IconResolver.Icon("lucide", "house", ""));
    assertThat(IconResolver.resolve("pi-check")).contains(new IconResolver.Icon("lucide", "check", ""));
  }

  @Test
  void resolvesLucideNamesAndAliases() {
    assertThat(IconResolver.resolve("lucide:house")).contains(new IconResolver.Icon("lucide", "house", ""));
    assertThat(IconResolver.resolve("lucide:activity-square"))
      .contains(new IconResolver.Icon("lucide", "square-activity", ""));
    assertThat(IconResolver.resolve("lucide:not-a-lucide-icon")).isEmpty();
  }

  @Test
  void keepsTheCurrentFontForNamesWithoutEquivalence() {
    assertThat(IconResolver.resolve("youtube-play")).contains(new IconResolver.Icon("fa", "youtube-play", ""));
    assertThat(IconResolver.resolve("mdi:not_mapped")).contains(new IconResolver.Icon("material", "not_mapped", ""));
    assertThat(IconResolver.resolve("constructor")).contains(new IconResolver.Icon("fa", "constructor", ""));
  }

  @Test
  void keepsTheExtraClassesAfterTheName() {
    assertThat(IconResolver.resolve(" refresh  fa-spin text-info "))
      .contains(new IconResolver.Icon("lucide", "refresh-cw", "fa-spin text-info"));
  }

  @Test
  void resolvesNothingWithoutIcon() {
    assertThat(IconResolver.resolve(null)).isEmpty();
    assertThat(IconResolver.resolve("  ")).isEmpty();
    assertThat(IconResolver.resolve("fa:")).isEmpty();
  }
}
