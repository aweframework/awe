package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.button.Button;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid quick filter model: the quick-filter element of a grid is a criterion the grid shows in its toolbar
 */
@DisplayName("Grid quick filter model")
class GridQuickFilterTest {

  private static final String USERS_GRID = "<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\">" +
    "<quick-filter id=\"CrtUsrSta\" initial-load=\"query\" target-action=\"UsrStaCnt\" value=\"\"/>" +
    "<column name=\"usr\" label=\"USER\"/><button id=\"BtnNew\" label=\"NEW\"/></grid>";

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    return (Grid) xstream.fromXML(xml);
  }

  @Test
  @DisplayName("The quick-filter is read from the XML as a criterion of the quick-filter component")
  void quickFilterIsParsed() {
    QuickFilter quickFilter = parse(USERS_GRID).getQuickFilter();

    assertEquals("CrtUsrSta", quickFilter.getId());
    assertEquals("query", quickFilter.getInitialLoad());
    assertEquals("UsrStaCnt", quickFilter.getTargetAction());
    assertEquals("", quickFilter.getValue());
    assertEquals("quick-filter", quickFilter.getComponentType());
  }

  @Test
  @DisplayName("A quick filter loads its options with the data server action unless it sets another one")
  void quickFilterServerActionDefaultsToData() {
    assertEquals("data", parse(USERS_GRID).getQuickFilter().getServerAction());
    assertEquals("data", new ObjectMapper().valueToTree(parse(USERS_GRID).getQuickFilter()).get("serverAction").asString());
    assertEquals("value", parse(USERS_GRID.replace("<quick-filter ", "<quick-filter server-action=\"value\" "))
      .getQuickFilter().getServerAction());
  }

  @Test
  @DisplayName("The grid sends the id of its quick filter, and nothing when it has none")
  void gridSendsItsQuickFilter() {
    JsonNode json = new ObjectMapper().valueToTree(parse(USERS_GRID));
    JsonNode plain = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><column name=\"a\"/></grid>"));

    assertEquals("CrtUsrSta", json.get("quickFilter").asString());
    assertFalse(plain.has("quickFilter"));
  }

  @Test
  @DisplayName("The quick filter is a component of the screen, not a column nor a button of the grid")
  void quickFilterIsAComponentOfTheScreen() {
    Grid grid = parse(USERS_GRID);

    assertTrue(grid.getElementsByType(Component.class).stream().anyMatch(QuickFilter.class::isInstance));
    assertEquals(List.of("usr"), grid.getElementsByType(Column.class).stream().map(Column::getName).toList());
    assertEquals(List.of("BtnNew"), grid.getButtonModelConverter().stream().map(Button::getId).toList());
  }

  @Test
  @DisplayName("The display of a quick filter (dropdown, date-range) is read from the XML and sent to the clients, and none by default")
  void quickFilterDisplayIsParsedAndSent() {
    QuickFilter dropdown = parse(USERS_GRID.replace("<quick-filter ", "<quick-filter display=\"dropdown\" ")).getQuickFilter();
    QuickFilter dateRange = parse(USERS_GRID.replace("<quick-filter ", "<quick-filter display=\"date-range\" ")).getQuickFilter();

    assertEquals("dropdown", dropdown.getDisplay());
    assertEquals("dropdown", new ObjectMapper().valueToTree(dropdown).get("display").asString());
    assertEquals("date-range", new ObjectMapper().valueToTree(dateRange).get("display").asString());
    assertEquals("date-range", ((QuickFilter) dateRange.copy()).getDisplay());
    assertFalse(new ObjectMapper().valueToTree(parse(USERS_GRID).getQuickFilter()).has("display"));
  }

  @Test
  @DisplayName("The quick filter renders nothing where it is declared: the grid shows it in its toolbar")
  void quickFilterHasNoTemplate() {
    QuickFilter quickFilter = parse(USERS_GRID).getQuickFilter();

    assertEquals(AweConstants.TEMPLATE_EMPTY, quickFilter.getTemplate());
    assertEquals("quick-filter", quickFilter.getComponentTag());
  }

  @Test
  @DisplayName("A grid with one quick-filter sends it as before, and as the only one of its quick filters")
  void oneQuickFilterIsSentAsBefore() {
    JsonNode json = new ObjectMapper().valueToTree(parse(USERS_GRID));

    assertEquals("CrtUsrSta", json.get("quickFilter").asString());
    assertEquals(List.of("CrtUsrSta"), json.get("quickFilters").valueStream().map(JsonNode::asString).toList());
    assertFalse(new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"/>")).has("quickFilters"));
  }

  @Test
  @DisplayName("Every quick-filter of a grid is used in the order of the XML, the first one is still its quick filter, and a copy keeps them")
  void everyQuickFilterIsUsedInOrderAndCopied() {
    Grid grid = parse(USERS_GRID.replace("<column ", "<quick-filter id=\"CrtOther\" initial-load=\"enum\" target-action=\"Es1Es0\"/><column "));

    assertEquals(List.of("CrtUsrSta", "CrtOther"), grid.getQuickFilters().stream().map(QuickFilter::getId).toList());
    assertEquals("CrtUsrSta", grid.getQuickFilter().getId());
    JsonNode json = new ObjectMapper().valueToTree(grid);
    assertEquals("CrtUsrSta", json.get("quickFilter").asString());
    assertEquals(List.of("CrtUsrSta", "CrtOther"), json.get("quickFilters").valueStream().map(JsonNode::asString).toList());
    assertEquals(List.of("CrtUsrSta", "CrtOther"), grid.copy().getQuickFilters().stream().map(QuickFilter::getId).toList());
    assertEquals("UsrStaCnt", ((QuickFilter) grid.getQuickFilter().copy()).getTargetAction());
    assertNull(parse("<grid id=\"Grd\"/>").getQuickFilter());
    assertEquals(List.of(), parse("<grid id=\"Grd\"/>").getQuickFilters());
  }
}
