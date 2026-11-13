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
 * Grid search model: the search element of a grid is a text criterion the grid shows in its toolbar
 */
@DisplayName("Grid search model")
class GridSearchTest {

  private static final String USERS_GRID = "<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\">" +
    "<search id=\"CrtUsrTxt\" placeholder=\"SCREEN_TEXT_USER_SEARCH\"/>" +
    "<quick-filter id=\"CrtUsrSta\" initial-load=\"query\" target-action=\"UsrStaCnt\"/>" +
    "<column name=\"usr\" label=\"USER\"/><button id=\"BtnNew\" label=\"NEW\"/></grid>";

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    return (Grid) xstream.fromXML(xml);
  }

  @Test
  @DisplayName("The search is read from the XML as a criterion of the grid-search component")
  void searchIsParsed() {
    GridSearch search = parse(USERS_GRID.replace("<search ", "<search label=\"SCREEN_TEXT_USER\" value=\"adm\" ")).getSearch();

    assertEquals("CrtUsrTxt", search.getId());
    assertEquals("SCREEN_TEXT_USER_SEARCH", search.getPlaceholder());
    assertEquals("SCREEN_TEXT_USER", search.getLabel());
    assertEquals("adm", search.getValue());
    assertEquals("grid-search", search.getComponentType());
  }

  @Test
  @DisplayName("The search has no query of its own: it sends no server action nor target action")
  void searchHasNoQuery() {
    JsonNode json = new ObjectMapper().valueToTree(parse(USERS_GRID).getSearch());

    assertEquals("grid-search", json.get("component").asString());
    assertEquals("SCREEN_TEXT_USER_SEARCH", json.get("placeholder").asString());
    assertFalse(json.has("serverAction"));
    assertFalse(json.has("targetAction"));
    assertNull(parse(USERS_GRID).getSearch().getInitialLoad());
  }

  @Test
  @DisplayName("The grid sends the id of its search next to its quick filter, and nothing when it has none")
  void gridSendsItsSearch() {
    JsonNode json = new ObjectMapper().valueToTree(parse(USERS_GRID));
    JsonNode plain = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><column name=\"a\"/></grid>"));

    assertEquals("CrtUsrTxt", json.get("search").asString());
    assertEquals("CrtUsrSta", json.get("quickFilter").asString());
    assertFalse(plain.has("search"));
  }

  @Test
  @DisplayName("The search is a component of the screen, not a column nor a button of the grid")
  void searchIsAComponentOfTheScreen() {
    Grid grid = parse(USERS_GRID);

    assertTrue(grid.getElementsByType(Component.class).stream().anyMatch(GridSearch.class::isInstance));
    assertEquals(List.of("usr"), grid.getElementsByType(Column.class).stream().map(Column::getName).toList());
    assertEquals(List.of("BtnNew"), grid.getButtonModelConverter().stream().map(Button::getId).toList());
  }

  @Test
  @DisplayName("The search renders nothing where it is declared: the grid shows it in its toolbar")
  void searchHasNoTemplate() {
    GridSearch search = parse(USERS_GRID).getSearch();

    assertEquals(AweConstants.TEMPLATE_EMPTY, search.getTemplate());
    assertEquals("grid-search", search.getComponentTag());
  }

  @Test
  @DisplayName("Only the first search of a grid is used, and a copy of the grid keeps it")
  void firstSearchIsUsedAndCopied() {
    Grid grid = parse(USERS_GRID.replace("<column ", "<search id=\"CrtOther\"/><column "));

    assertEquals("CrtUsrTxt", grid.getSearch().getId());
    assertEquals("CrtUsrTxt", grid.copy().getSearch().getId());
    assertEquals("SCREEN_TEXT_USER_SEARCH", ((GridSearch) grid.getSearch().copy()).getPlaceholder());
    assertNull(parse("<grid id=\"Grd\"/>").getSearch());
  }
}
