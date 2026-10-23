package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid row detail model: the row-detail element of the XML reaches the clients as the rowDetail attribute
 */
@DisplayName("Grid row detail model")
class GridRowDetailTest {

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    return (Grid) xstream.fromXML(xml);
  }

  // Parses with the driver of the framework (DOM), which reads the text of a node differently than the default one
  private static Grid parseDom(String xml) {
    return (Grid) XmlSerializerTestUtil.buildMarshaller().getXStream().fromXML(xml);
  }

  // Parses with one of the drivers the framework can be configured with
  private static Grid parse(String driver, String xml) {
    return switch (driver) {
      case "dom" -> parseDom(xml);
      case "stax" -> {
        XStream xstream = new XStream(new StaxDriver());
        xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
        xstream.processAnnotations(Grid.class);
        yield (Grid) xstream.fromXML(xml);
      }
      default -> parse(xml);
    };
  }

  private static final String ORDERS_GRID = "<grid id=\"GrdOrders\" server-action=\"data\" target-action=\"QryOrders\" initial-load=\"query\">" +
    "<column name=\"order\"/><column name=\"status\"/></grid>";

  @Test
  @DisplayName("The row-detail body is read from the XML and sent as the template")
  void rowDetailTemplateIsParsedAndSerialized() {
    Grid grid = parse("<grid id=\"Grd\"><column name=\"name\"/><column name=\"notes\" hidden=\"true\"/>" +
      "<row-detail><![CDATA[Name: {name}\nNotes: <{notes}>]]></row-detail></grid>");

    JsonNode json = new ObjectMapper().valueToTree(grid);

    assertEquals("Name: {name}\nNotes: <{notes}>", json.get("rowDetail").get("template").asString());
    assertEquals(2, grid.getElementsByType(Column.class).size());
  }

  @Test
  @DisplayName("The style of the row-detail is sent with its template, and only when it is set")
  void rowDetailStyleIsSerialized() {
    JsonNode styled = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><row-detail style=\"detail-card bg-soft\">{name}</row-detail></grid>"));
    JsonNode plain = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><row-detail>{name}</row-detail></grid>"));

    assertEquals("detail-card bg-soft", styled.get("rowDetail").get("style").asString());
    assertEquals("{name}", styled.get("rowDetail").get("template").asString());
    assertNull(plain.get("rowDetail").get("style"));
  }

  @Test
  @DisplayName("A grid without row-detail sends no rowDetail attribute")
  void gridWithoutRowDetail() {
    JsonNode json = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><column name=\"name\"/></grid>"));

    assertTrue(json.get("rowDetail") == null || json.get("rowDetail").isNull());
  }

  @Test
  @DisplayName("Copying a grid keeps its row detail")
  void copyKeepsRowDetail() {
    Grid grid = parse("<grid id=\"Grd\"><row-detail>{name}</row-detail></grid>");

    Grid copy = grid.copy();

    assertEquals("{name}", copy.getRowDetail().getContent());
    assertNull(parse("<grid id=\"Grd\"/>").getRowDetail());
  }

  @ParameterizedTest
  @ValueSource(strings = {"xpp", "dom", "stax"})
  @DisplayName("A grid inside the row-detail is read as its component, with no template")
  void rowDetailWithAGridIsParsed(String driver) {
    String xml = "<grid id=\"GrdProducts\"><column name=\"id\"/><row-detail style=\"detail-card\">\n  " + ORDERS_GRID +
      "\n</row-detail></grid>";
    Grid grid = parse(driver, xml);

    RowDetail rowDetail = grid.getRowDetail();

    assertTrue(rowDetail.isComponentMode());
    assertNull(rowDetail.getContent());
    assertEquals("detail-card", rowDetail.getStyle());
    assertEquals("GrdOrders", rowDetail.getDetailGrid().getId());
    assertNull(rowDetail.getDefinitionError());
  }

  @ParameterizedTest
  @ValueSource(strings = {"xpp", "dom", "stax"})
  @DisplayName("The text of the row-detail is still read as its template with every driver")
  void rowDetailTemplateIsParsedWithEveryDriver(String driver) {
    Grid grid = parse(driver, "<grid id=\"Grd\"><row-detail style=\"detail-card\"><![CDATA[Name: {name}\n]]></row-detail></grid>");

    assertEquals("Name: {name}\n", grid.getRowDetail().getContent());
    assertEquals("detail-card", grid.getRowDetail().getStyle());
    assertFalse(grid.getRowDetail().isComponentMode());
    assertNull(grid.getRowDetail().getDefinitionError());
  }

  @Test
  @DisplayName("The components of the row-detail are not part of the grid: its columns, grids and components are its own")
  void rowDetailComponentsAreHiddenFromTheGrid() {
    Grid grid = parse("<grid id=\"GrdProducts\"><column name=\"id\"/><row-detail>" + ORDERS_GRID + "</row-detail></grid>");

    assertEquals(1, grid.getElementsByType(Column.class).size());
    assertTrue(grid.getElementsByType(Component.class).stream().noneMatch(Grid.class::isInstance));
    assertEquals(List.of("GrdOrders", "order", "status"), grid.getRowDetail().getDetailElements().stream()
      .map(element -> element instanceof Column column ? column.getName() : element.getId()).toList());
  }

  @Test
  @DisplayName("An include inside the row-detail is its component once resolved to a single grid")
  void rowDetailWithAnIncludeIsParsed() {
    Grid grid = parse("<grid id=\"GrdProducts\"><row-detail><include target-screen=\"orders\" target-source=\"detail\"/></row-detail></grid>");
    RowDetail rowDetail = grid.getRowDetail();
    Include include = (Include) rowDetail.getElementList().get(0);

    assertTrue(rowDetail.isComponentMode());
    assertEquals("orders", include.getTargetScreen());
    assertEquals("detail", include.getTargetSource());

    include.setElementList(List.of(parse(ORDERS_GRID)));

    assertEquals("GrdOrders", rowDetail.getDetailGrid().getId());
    assertNull(rowDetail.getDefinitionError());
  }

  @ParameterizedTest
  @ValueSource(strings = {"xpp", "dom", "stax"})
  @DisplayName("A row-detail with a template and components is parsed with both and rejected")
  void rowDetailWithTextAndComponentsIsRejected(String driver) {
    String xml = "<grid id=\"Grd\"><row-detail>Name: {name}" + ORDERS_GRID + "</row-detail></grid>";
    RowDetail rowDetail = parse(driver, xml).getRowDetail();

    assertEquals("Name: {name}", rowDetail.getContent());
    assertEquals(RowDetail.DefinitionError.TEMPLATE_AND_COMPONENTS, rowDetail.getDefinitionError());
  }

  @Test
  @DisplayName("A row-detail with more than one component, or with a component that is not a grid, is rejected")
  void rowDetailWithAnythingButOneGridIsRejected() {
    assertEquals(RowDetail.DefinitionError.NOT_A_GRID, parse("<grid id=\"Grd\"><row-detail>" + ORDERS_GRID +
      ORDERS_GRID.replace("GrdOrders", "GrdOther") + "</row-detail></grid>").getRowDetail().getDefinitionError());
    assertEquals(RowDetail.DefinitionError.NOT_A_GRID, parse("<grid id=\"Grd\"><row-detail><criteria id=\"Crt\"/></row-detail></grid>")
      .getRowDetail().getDefinitionError());
    assertEquals(RowDetail.DefinitionError.NOT_A_GRID, parse("<grid id=\"Grd\"><row-detail><pivot-table id=\"Pvt\"/></row-detail></grid>")
      .getRowDetail().getDefinitionError());

    RowDetail withInclude = parse("<grid id=\"Grd\"><row-detail><include target-screen=\"s\" target-source=\"t\"/></row-detail></grid>").getRowDetail();
    ((Include) withInclude.getElementList().get(0)).setElementList(List.of(parse(ORDERS_GRID), parse(ORDERS_GRID.replace("GrdOrders", "GrdOther"))));
    assertEquals(RowDetail.DefinitionError.NOT_A_GRID, withInclude.getDefinitionError());
    assertNull(withInclude.getDetailGrid());
  }

  @ParameterizedTest
  @ValueSource(strings = {"editable=\"true\"", "send-operations=\"true\""})
  @DisplayName("The grid of a row-detail is read-only")
  void rowDetailWithAnEditableGridIsRejected(String attribute) {
    RowDetail rowDetail = parse("<grid id=\"Grd\"><row-detail>" + ORDERS_GRID.replace("<grid ", "<grid " + attribute + " ") +
      "</row-detail></grid>").getRowDetail();

    assertEquals(RowDetail.DefinitionError.EDITABLE_GRID, rowDetail.getDefinitionError());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "<grid id=\"GrdOrders\"><column name=\"order\"/><dependency><dependency-element id=\"Crt\"/></dependency></grid>",
    "<grid id=\"GrdOrders\"><column name=\"order\"><dependency><dependency-element id=\"Crt\"/></dependency></column></grid>",
    "<grid id=\"GrdOrders\"><button id=\"Btn\"><dependency><dependency-element id=\"Crt\"/></dependency></button></grid>"})
  @DisplayName("A dependency anywhere inside the row-detail is rejected")
  void rowDetailWithADependencyIsRejected(String nestedGrid) {
    RowDetail rowDetail = parse("<grid id=\"Grd\"><row-detail>" + nestedGrid + "</row-detail></grid>").getRowDetail();

    assertEquals(RowDetail.DefinitionError.DEPENDENCY, rowDetail.getDefinitionError());
  }

  @Test
  @DisplayName("In components mode the rowDetail sends its style and the generated component, never a template")
  void rowDetailComponentIsSerialized() {
    Grid grid = parse("<grid id=\"GrdProducts\"><row-detail style=\"detail-card\">" + ORDERS_GRID + "</row-detail></grid>");

    JsonNode notGenerated = new ObjectMapper().valueToTree(grid).get("rowDetail");
    grid.getRowDetail().setComponent(new ScreenComponent().setId("GrdOrders").setController(grid.getRowDetail().getDetailGrid())
      .setModel(new ComponentModel()));
    JsonNode generated = new ObjectMapper().valueToTree(grid).get("rowDetail");

    assertEquals("detail-card", notGenerated.get("style").asString());
    assertNull(notGenerated.get("template"));
    assertNull(notGenerated.get("component"));
    assertNull(generated.get("template"));
    assertEquals("GrdOrders", generated.get("component").get("id").asString());
    assertEquals("QryOrders", generated.get("component").get("controller").get("targetAction").asString());
    assertTrue(generated.get("component").has("model"));
  }

  @Test
  @DisplayName("Copying a grid copies the components of its row detail, so resolving a copy never changes the original")
  void copyCopiesTheRowDetailComponents() {
    Grid grid = parse("<grid id=\"GrdProducts\"><row-detail style=\"detail-card\">" + ORDERS_GRID + "</row-detail></grid>");

    Grid copy = grid.copy();

    assertNotSame(grid.getRowDetail(), copy.getRowDetail());
    assertNotSame(grid.getRowDetail().getDetailGrid(), copy.getRowDetail().getDetailGrid());
    assertEquals("GrdOrders", copy.getRowDetail().getDetailGrid().getId());
    assertEquals("detail-card", copy.getRowDetail().getStyle());
  }

  @ParameterizedTest
  @ValueSource(strings = {"<row-detail style=\"card\">Name: {name}</row-detail>", "<row-detail style=\"card\">" + ORDERS_GRID + "</row-detail>"})
  @DisplayName("A row-detail is written back to XML as it was read")
  void rowDetailIsMarshalled(String rowDetailXml) {
    XStream xstream = XmlSerializerTestUtil.buildMarshaller().getXStream();
    Grid grid = parseDom("<grid id=\"Grd\">" + rowDetailXml + "</grid>");

    Grid again = (Grid) xstream.fromXML(xstream.toXML(grid));

    assertEquals(grid.getRowDetail().getContent(), again.getRowDetail().getContent());
    assertEquals("card", again.getRowDetail().getStyle());
    assertEquals(grid.getRowDetail().getElementList().size(), again.getRowDetail().getElementList().size());
    assertSame(grid.getRowDetail().isComponentMode(), again.getRowDetail().isComponentMode());
  }
}
