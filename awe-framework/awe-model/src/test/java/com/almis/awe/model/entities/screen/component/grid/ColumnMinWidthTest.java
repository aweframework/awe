package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Minimum width of the column that shares the free space of a grid (width="*", #993): it is read from the XML and
 * reaches the clients, and a column without it sends nothing new
 */
class ColumnMinWidthTest {

  @Test
  void theMinimumWidthIsReadFromTheXmlAndReachesTheClients() {
    XStream xStream = new XStream();
    xStream.processAnnotations(Column.class);
    xStream.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    Column column = (Column) xStream.fromXML("<column name=\"TskNam\" width=\"*\" min-width=\"200\"/>");

    assertEquals(200, column.getMinWidth());
    assertEquals(200, JsonMapper.builder().build().valueToTree(column).get("minWidth").asInt());
    JsonNode without = JsonMapper.builder().build().valueToTree(new Column());
    assertFalse(without.has("minWidth"));
  }
}
