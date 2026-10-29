package com.almis.awe.model.entities.screen.component.button;

import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selection buttons of a grid (#793): a grid button with selection="true" is shown in the selection bar of the grid,
 * not in its footer
 */
class ButtonSelectionTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static XStream xstream() {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Button.class, Grid.class});
    return xstream;
  }

  private static Button parse(String xml) {
    return (Button) xstream().fromXML(xml);
  }

  @Test
  void aButtonIsNotASelectionButtonByDefault() {
    Button button = parse("<button id=\"ButNew\" label=\"BUTTON_NEW\"/>");

    assertThat(button.getSelection()).isNull();
    // The clients receive nothing new: the button stays in the footer
    assertThat(mapper.valueToTree(button).has("selection")).isFalse();
  }

  @Test
  void selectionTrueReachesTheClients() {
    Button button = parse("<button id=\"ButDel\" label=\"BUTTON_DELETE\" style=\"btn-danger\" selection=\"true\"/>");

    assertThat(button.getSelection()).isTrue();
    JsonNode json = mapper.valueToTree(button);
    assertThat(json.get("selection").asBoolean()).isTrue();
    assertThat(json.get("style").asString()).isEqualTo("btn-danger");
  }

  @Test
  void theButtonModelOfTheGridKeepsTheSelectionButtonsInTheirOrder() {
    Grid grid = (Grid) xstream().fromXML("<grid id=\"GrdUsr\" multiselect=\"true\">"
      + "<button id=\"ButNew\"/><button id=\"ButRst\" selection=\"true\"/><button id=\"ButDel\" selection=\"true\"/></grid>");

    JsonNode buttons = mapper.valueToTree(grid).get("buttonModel");
    assertThat(buttons).hasSize(3);
    assertThat(buttons.get(0).get("id").asString()).isEqualTo("ButNew");
    assertThat(buttons.get(0).has("selection")).isFalse();
    assertThat(buttons.get(1).get("id").asString()).isEqualTo("ButRst");
    assertThat(buttons.get(1).get("selection").asBoolean()).isTrue();
    assertThat(buttons.get(2).get("id").asString()).isEqualTo("ButDel");
    assertThat(buttons.get(2).get("selection").asBoolean()).isTrue();
  }

  @Test
  void aCopyKeepsTheAttribute() {
    Button button = parse("<button id=\"ButDel\" selection=\"true\"/>");

    assertThat(button.copy().getSelection()).isTrue();
  }
}
