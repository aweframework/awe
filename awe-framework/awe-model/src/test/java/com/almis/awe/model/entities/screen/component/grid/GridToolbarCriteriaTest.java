package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.screen.component.Component;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Toolbar criteria of a grid: criteria of any component that the grid shows at the end of its toolbar, before its
 * Columns button
 */
@DisplayName("Grid toolbar criteria model")
class GridToolbarCriteriaTest {

  private static final String GRID = "<grid id=\"GrdExe\" server-action=\"data\" target-action=\"ExeLst\">" +
    "<search id=\"CrtTxt\"/>" +
    "<toolbar-criteria id=\"CrtGrp\" component=\"checkbox\" style=\"switch no-label\" label=\"GROUP_BY_FLOW\">" +
    "<dependency><dependency-element id=\"CrtGrp\" condition=\"eq\" value=\"1\"/>" +
    "<dependency-action type=\"filter\" target=\"GrdExe\"/></dependency></toolbar-criteria>" +
    "<toolbar-criteria id=\"CrtViw\" component=\"select\" initial-load=\"enum\" target-action=\"Views\"/>" +
    "<column name=\"a\" label=\"A\"/></grid>";

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    return (Grid) xstream.fromXML(xml);
  }

  @Test
  @DisplayName("A toolbar criterion is a criterion of its own component, with its dependencies, and renders nothing where it is declared")
  void toolbarCriterionIsACriterionOfItsComponent() {
    GridToolbarCriteria criterion = parse(GRID).getToolbarCriteria().get(0);

    assertEquals("CrtGrp", criterion.getId());
    assertEquals("checkbox", criterion.getComponentType());
    assertEquals("input-checkbox", criterion.getComponentTag());
    assertEquals("switch no-label", criterion.getStyle());
    assertEquals(1, criterion.getDependencyConverter().size());
    assertEquals(AweConstants.TEMPLATE_EMPTY, criterion.getTemplate());
    assertTrue(parse(GRID).getElementsByType(Component.class).contains(criterion), "a component of the screen");
  }

  @Test
  @DisplayName("The grid sends the ids of its toolbar criteria in order, and nothing when it has none; a copy keeps them")
  void gridSendsItsToolbarCriteria() {
    Grid grid = parse(GRID);
    JsonNode json = new ObjectMapper().valueToTree(grid);
    JsonNode plain = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><column name=\"a\"/></grid>"));

    assertEquals(List.of("CrtGrp", "CrtViw"), json.get("toolbarCriteria").valueStream().map(JsonNode::asString).toList());
    assertEquals("CrtTxt", json.get("search").asString());
    assertFalse(plain.has("toolbarCriteria"));
    assertEquals(List.of("CrtGrp", "CrtViw"), grid.copy().getToolbarCriteriaConverter());
    assertEquals(List.of("a"), grid.getElementsByType(Column.class).stream().map(Column::getName).toList());
  }
}
