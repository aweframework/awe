package com.almis.awe.model.entities.screen.component.button;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Busy state of a button: the button that launched an action shows a spinner while it runs, unless show-loading is false
 * (#771)
 */
class ButtonShowLoadingTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Button parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Button.class);
    return (Button) xstream.fromXML(xml);
  }

  @Test
  void showsTheBusyStateByDefault() {
    Button button = parse("<button id=\"ButSav\" label=\"BUTTON_SAVE\"/>");

    assertThat(button.getShowLoading()).isNull();
    assertThat(button.showsLoading()).isTrue();
    // The clients read an absent attribute as true
    assertThat(mapper.valueToTree(button).has("showLoading")).isFalse();
  }

  @Test
  void showLoadingFalseTurnsTheBusyStateOff() {
    Button button = parse("<button id=\"ButFlt\" show-loading=\"false\"/>");

    assertThat(button.showsLoading()).isFalse();
    JsonNode json = mapper.valueToTree(button);
    assertThat(json.get("showLoading").asBoolean()).isFalse();
  }

  @Test
  void showLoadingTrueKeepsTheBusyState() {
    Button button = parse("<button id=\"ButSav\" show-loading=\"true\"/>");

    assertThat(button.showsLoading()).isTrue();
    assertThat(mapper.valueToTree(button).get("showLoading").asBoolean()).isTrue();
  }

  @Test
  void aCopyKeepsTheAttribute() {
    Button button = parse("<button id=\"ButFlt\" show-loading=\"false\"/>");

    assertThat(button.copy().getShowLoading()).isFalse();
  }
}
