package com.almis.awe.model.entities.screen.component;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Side panel container: a dialog anchored to the left or right edge of the screen (#843)
 */
class SidePanelTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static SidePanel parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(SidePanel.class);
    return (SidePanel) xstream.fromXML(xml);
  }

  @Test
  void isOpenedAndClosedAsADialog() {
    SidePanel sidePanel = new SidePanel();

    assertThat(sidePanel).isInstanceOf(Dialog.class);
    assertThat(sidePanel.getComponentTag()).isEqualTo("side-panel");
  }

  @Test
  void attributesAreReadFromTheXml() {
    SidePanel sidePanel = parse("<side-panel id=\"Pnl\" label=\"LABEL\" position=\"left\" max-width=\"30rem\" " +
      "deferred=\"true\" on-close=\"reject\"/>");

    assertThat(sidePanel.getId()).isEqualTo("Pnl");
    assertThat(sidePanel.getPosition()).isEqualTo("left");
    assertThat(sidePanel.getMaxWidth()).isEqualTo("30rem");
    assertThat(sidePanel.isDeferred()).isTrue();
    assertThat(sidePanel.acceptOnClose()).isFalse();
  }

  @Test
  void clientReceivesTheRightEdgeByDefault() {
    JsonNode json = mapper.valueToTree(new SidePanel());

    assertThat(json.get("elementType").asString()).isEqualTo("SidePanel");
    assertThat(json.get("position").asString()).isEqualTo("right");
    assertThat(json.has("maxWidth")).isFalse();
    assertThat(json.has("deferred")).isFalse();
    assertThat(json.get("acceptOnClose").asBoolean()).isTrue();
  }

  @Test
  void clientReceivesThePositionAndMaxWidth() {
    SidePanel sidePanel = new SidePanel();
    sidePanel.setPosition("left");
    sidePanel.setMaxWidth("480px");

    JsonNode json = mapper.valueToTree(sidePanel);

    assertThat(json.get("position").asString()).isEqualTo("left");
    assertThat(json.get("maxWidth").asString()).isEqualTo("480px");
  }

  @Test
  void attributesSurviveACopy() {
    SidePanel sidePanel = new SidePanel();
    sidePanel.setPosition("left");
    sidePanel.setMaxWidth("480px");
    sidePanel.setDeferred(true);

    SidePanel copy = sidePanel.copy();

    assertThat(copy.getPosition()).isEqualTo("left");
    assertThat(copy.getMaxWidth()).isEqualTo("480px");
    assertThat(copy.isDeferred()).isTrue();
  }
}
