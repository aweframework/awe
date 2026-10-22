package com.almis.awe.model.entities.screen.component;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Popover container: a dialog anchored to a control of the screen (#843)
 */
class PopoverTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Popover parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Popover.class);
    return (Popover) xstream.fromXML(xml);
  }

  @Test
  void isOpenedAndClosedAsADialog() {
    Popover popover = new Popover();

    assertThat(popover).isInstanceOf(Dialog.class);
    assertThat(popover.getComponentTag()).isEqualTo("popover");
  }

  @Test
  void attributesAreReadFromTheXml() {
    Popover popover = parse("<popover id=\"Pop\" label=\"LABEL\" anchor=\"BtnInf\" position=\"right\" " +
      "max-width=\"20rem\" deferred=\"true\" on-close=\"reject\"/>");

    assertThat(popover.getId()).isEqualTo("Pop");
    assertThat(popover.getAnchor()).isEqualTo("BtnInf");
    assertThat(popover.getPosition()).isEqualTo("right");
    assertThat(popover.getMaxWidth()).isEqualTo("20rem");
    assertThat(popover.isDeferred()).isTrue();
    assertThat(popover.acceptOnClose()).isFalse();
  }

  @Test
  void clientReceivesTheBottomPositionByDefault() {
    JsonNode json = mapper.valueToTree(new Popover());

    assertThat(json.get("elementType").asString()).isEqualTo("Popover");
    assertThat(json.get("position").asString()).isEqualTo("bottom");
    assertThat(json.has("anchor")).isFalse();
    assertThat(json.has("maxWidth")).isFalse();
    assertThat(json.has("deferred")).isFalse();
    assertThat(json.get("acceptOnClose").asBoolean()).isTrue();
  }

  @Test
  void clientReceivesTheAnchorPositionAndMaxWidth() {
    Popover popover = new Popover();
    popover.setAnchor("BtnInf");
    popover.setPosition("top");
    popover.setMaxWidth("320px");

    JsonNode json = mapper.valueToTree(popover);

    assertThat(json.get("anchor").asString()).isEqualTo("BtnInf");
    assertThat(json.get("position").asString()).isEqualTo("top");
    assertThat(json.get("maxWidth").asString()).isEqualTo("320px");
  }

  @Test
  void attributesSurviveACopy() {
    Popover popover = new Popover();
    popover.setAnchor("BtnInf");
    popover.setPosition("left");
    popover.setMaxWidth("320px");
    popover.setDeferred(true);

    Popover copy = popover.copy();

    assertThat(copy.getAnchor()).isEqualTo("BtnInf");
    assertThat(copy.getPosition()).isEqualTo("left");
    assertThat(copy.getMaxWidth()).isEqualTo("320px");
    assertThat(copy.isDeferred()).isTrue();
  }
}
