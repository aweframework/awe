package com.almis.awe.model.entities.screen.component;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Theme preview: a sample of the theme the criteria it names describe, before it is saved (#949)
 */
class ThemePreviewTest {

  private static final String XML = "<theme-preview id=\"ThmPrv\" label=\"SCREEN_TEXT_THEME_PREVIEW\" " +
    "primary=\"PriPal\" primary-hex=\"PriHex\" surface=\"SrfPal\" surface-hex=\"SrfHex\" mode=\"Mde\"/>";

  private static ThemePreview parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(ThemePreview.class);
    return (ThemePreview) xstream.fromXML(xml);
  }

  @Test
  void parsesTheCriteriaItReadsAndReachesTheClientInCamelCase() {
    ThemePreview preview = parse(XML);

    assertThat(preview).isInstanceOf(Component.class);
    assertThat(preview.getComponentTag()).isEqualTo("theme-preview");
    assertThat(preview.getId()).isEqualTo("ThmPrv");
    assertThat(preview.getLabel()).isEqualTo("SCREEN_TEXT_THEME_PREVIEW");

    JsonNode json = new ObjectMapper().valueToTree(preview.copy());
    assertThat(json.get("elementType").asString()).isEqualTo("ThemePreview");
    assertThat(json.get("primary").asString()).isEqualTo("PriPal");
    assertThat(json.get("primaryHex").asString()).isEqualTo("PriHex");
    assertThat(json.get("surface").asString()).isEqualTo("SrfPal");
    assertThat(json.get("surfaceHex").asString()).isEqualTo("SrfHex");
    assertThat(json.get("mode").asString()).isEqualTo("Mde");
  }
}
