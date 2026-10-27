package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.constants.SecurityEndpoints;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The settings and the theme appearance of the session are public web resources; nothing else under /settings is
 */
class SecurityEndpointsTest {

  private final SecurityEndpoints endpoints = new SecurityEndpoints();

  private boolean isWebResource(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
    return Arrays.stream(endpoints.getWebResourcesRequestMatchers()).anyMatch(matcher -> matcher.matches(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/settings", "/settings/appearance"})
  void theSettingsAndTheAppearanceArePublic(String path) {
    assertThat(isWebResource(path)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/settings/other", "/settings/appearance/other", "/settings/appearances", "/settings/"})
  void noOtherPathUnderTheSettingsIsPublic(String path) {
    assertThat(isWebResource(path)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/icons/lucide-sprite.svg"})
  void theIconSpriteIsAPublicWebResource(String path) {
    // The Lucide icons (#885) are drawn from this sprite on every page, the sign in included
    assertThat(isWebResource(path)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/iconsx/lucide-sprite.svg", "/static/icons/lucide-sprite.svg"})
  void nothingElseNamedLikeTheIconsIsPublic(String path) {
    assertThat(isWebResource(path)).isFalse();
  }
}
