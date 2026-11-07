package com.almis.awe.model.entities.screen.component.button;

import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The permission attributes of the screen elements: {@code permission} on buttons, context buttons and columns, and
 * {@code edit-permission} on the button actions. They are read by the server only; the clients receive the view mode
 * flag of a dialog action
 */
class ElementPermissionTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Object parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Button.class, ContextButton.class, Column.class, ButtonAction.class});
    return xstream.fromXML(xml);
  }

  @Test
  void buttonsAndColumnsReadThePermissionAndDoNotSendIt() {
    Button button = (Button) parse("<button id=\"ButDel\" permission=\"users.read users.delete\"/>");
    ContextButton contextButton = (ContextButton) parse("<context-button id=\"CtxDel\" permission=\"users.delete\"/>");
    Column column = (Column) parse("<column name=\"DelBtn\" component=\"button\" permission=\"users.delete\"/>");

    assertThat(button.getPermission()).isEqualTo("users.read users.delete");
    assertThat(button.copy().getPermission()).isEqualTo("users.read users.delete");
    assertThat(contextButton.copy().getPermission()).isEqualTo("users.delete");
    assertThat(column.copy().getPermission()).isEqualTo("users.delete");
    assertThat(mapper.valueToTree(button).has("permission")).isFalse();
    assertThat(mapper.valueToTree(column).has("permission")).isFalse();
  }

  @Test
  void aDialogActionReadsTheEditPermissionAndSendsOnlyTheViewMode() {
    ButtonAction action = (ButtonAction) parse("<button-action type=\"dialog\" target=\"PnlUsr\" "
      + "edit-permission=\"users.update\"/>");

    assertThat(action.getEditPermission()).isEqualTo("users.update");
    JsonNode json = mapper.valueToTree(action);
    assertThat(json.has("editPermission")).isFalse();
    assertThat(json.has("viewMode")).isFalse();

    ButtonAction viewMode = action.copy().setViewMode(true);
    assertThat(viewMode.getEditPermission()).isEqualTo("users.update");
    assertThat(mapper.valueToTree(viewMode).get("viewMode").asBoolean()).isTrue();
    assertThat(mapper.valueToTree(viewMode.copy()).get("viewMode").asBoolean()).isTrue();
  }
}
