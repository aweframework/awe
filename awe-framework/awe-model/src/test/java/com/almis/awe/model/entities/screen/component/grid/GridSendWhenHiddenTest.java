package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A grid sends its values while it is hidden unless it declares send-when-hidden="false"
 */
class GridSendWhenHiddenTest {

  @Test
  @DisplayName("send-when-hidden is true by default, read from the XML, kept by a copy and sent to the clients")
  void sendWhenHiddenIsReadCopiedAndSent() {
    assertTrue(new ObjectMapper().valueToTree(new Grid()).get("sendWhenHidden").asBoolean());

    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    Grid grid = (Grid) xstream.fromXML("<grid id=\"Grd\" send-when-hidden=\"false\"/>");

    assertFalse(grid.isSendWhenHidden());
    assertFalse(grid.copy().isSendWhenHidden());
    assertFalse(new ObjectMapper().valueToTree(grid).get("sendWhenHidden").asBoolean());
  }
}
