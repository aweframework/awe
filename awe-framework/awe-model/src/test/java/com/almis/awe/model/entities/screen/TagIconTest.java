package com.almis.awe.model.entities.screen;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Icon of a tag (#793): a heading tag with a label can show an icon before it, as the section titles of a form
 */
class TagIconTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Tag parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Tag.class});
    return (Tag) xstream.fromXML(xml);
  }

  @Test
  void aTagHasNoIconByDefault() {
    Tag tag = parse("<tag type=\"h3\" label=\"SCREEN_TEXT_USER_IDENTITY\"/>");

    assertThat(tag.getIcon()).isNull();
    // The clients receive nothing new
    assertThat(mapper.valueToTree(tag).has("icon")).isFalse();
  }

  @Test
  void theIconReachesTheClients() {
    Tag tag = parse("<tag type=\"h3\" label=\"SCREEN_TEXT_USER_IDENTITY\" icon=\"user\" style=\"section-title\"/>");

    assertThat(tag.getIcon()).isEqualTo("user");
    JsonNode json = mapper.valueToTree(tag);
    assertThat(json.get("icon").asString()).isEqualTo("user");
    assertThat(json.get("style").asString()).isEqualTo("section-title");
  }

  @Test
  void aCopyKeepsTheIcon() {
    Tag tag = parse("<tag type=\"h3\" label=\"SCREEN_TEXT_USER_ACCESS\" icon=\"lucide:shield\"/>");

    assertThat(tag.copy().getIcon()).isEqualTo("lucide:shield");
  }
}
