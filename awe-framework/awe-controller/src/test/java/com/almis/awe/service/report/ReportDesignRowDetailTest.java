package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.grid.ReportColumn;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Print design of the row detail of a grid (#929): off unless the row-detail asks for it with print="true"
 */
class ReportDesignRowDetailTest extends AbstractReportDesignTest {

  private static final List<PrintColumnData> COLUMNS = List.of(
    new PrintColumnData().setName("name").setLabel("Name"),
    new PrintColumnData().setName("city").setLabel("City"));

  /** Two rows; the id is sent (it is a column) but not printed */
  private ObjectNode orders() {
    return parameters(COLUMNS, data -> {
      field(data, "id", cell(7, "7"), cell(8, "8"));
      field(data, "name", cell("Ann", "Ann"), cell("Bob", "Bob"));
      field(data, "city", cell("Oslo", "Oslo"), cell("Rome", "Rome City"));
    });
  }

  private Grid ordersGrid(Consumer<Grid> customizer) throws Exception {
    return grid(customizer, "id", "name", "city");
  }

  private static RowDetail detail(boolean print, String template) {
    return new RowDetail().setPrint(print).setContent(template);
  }

  private Grid nestedGrid(Integer max) throws Exception {
    Grid nested = new GridBuilder().setId("GrdLines").build();
    nested.setTargetAction("QryLines").setMax(max);
    for (String name : List.of("item", "secret", "note")) {
      nested.addElement(new TextColumnBuilder().setName(name).setLabel(name.toUpperCase()).build());
    }
    nested.getElementsByType(Column.class).forEach(column -> {
      column.setComponentType(null);
      if ("secret".equals(column.getName())) {
        column.setPrintable("false");
      }
      if ("note".equals(column.getName())) {
        column.setHidden(true);
      }
    });
    return nested;
  }

  private static DataList lines(String... items) {
    DataList list = new DataList();
    for (String item : items) {
      Map<String, CellData> row = new HashMap<>();
      row.put("item", new CellData(item));
      row.put("secret", new CellData("s"));
      list.addRow(row);
    }
    return list;
  }

  @Test
  void textTemplateIsRenderedPerRowIntoTheRowDetailField() throws Exception {
    Grid grid = ordersGrid(g -> g.addElement(detail(true, "Order {id}: {name} lives in {city} {nothing}")));

    ReportGrid reportGrid = design(grid, orders(), false);

    assertEquals("_row_detail_", reportGrid.getRowDetailField());
    assertEquals(List.of("name", "city", "_row_detail_"), reportGrid.getFields());
    assertEquals(List.of("Order 7: Ann lives in Oslo ", "Order 8: Bob lives in Rome City "), reportGrid.getData().stream()
      .map(row -> row.get(2)).toList());
    assertNull(reportGrid.getRowDetailGrid());
  }

  @Test
  void templateFollowsTheSamePlaceholderRulesAsTheClients() {
    String text = RowDetailText.render("{a} {b.c-d} {not a name} {{a} {a", name -> "<" + name + ">");

    assertEquals("<a> <b.c-d> {not a name} {<a> {a", text);
  }

  @Test
  void rowDetailIsNotPrintedUnlessTheGridAsksForIt() throws Exception {
    ReportGrid off = design(ordersGrid(g -> g.addElement(detail(false, "{name}"))), orders(), false);
    ReportGrid unset = design(ordersGrid(g -> g.addElement(new RowDetail().setContent("{name}"))), orders(), false);
    ReportGrid none = design(ordersGrid(g -> {}), orders(), false);

    for (ReportGrid reportGrid : List.of(off, unset, none)) {
      assertNull(reportGrid.getRowDetailField());
      assertEquals(List.of("name", "city"), reportGrid.getFields());
    }
  }

  @Test
  void nestedGridRunsItsQueryPerRowWithTheParentRowAndIsCappedByItsMax() throws Exception {
    Grid nested = nestedGrid(1);
    Grid grid = ordersGrid(g -> g.addElement(new RowDetail().setPrint(true).addElement(nested)));
    when(queryService.launchPrivateQuery(eq("QryLines"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(lines("bolt", "nut")))
      .thenReturn(new ServiceData().setDataList(lines("screw")));

    ReportGrid reportGrid = design(grid, orders(), false);

    ArgumentCaptor<ObjectNode> query = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, times(2)).launchPrivateQuery(eq("QryLines"), query.capture());
    assertEquals(7, query.getAllValues().get(0).get("gridId.row.id").asInt());
    assertEquals("Rome", query.getAllValues().get(1).get("gridId.row.city").asString());
    assertEquals(1, query.getAllValues().get(0).get("max").asInt());

    ReportGrid template = reportGrid.getRowDetailGrid();
    assertEquals(List.of("item"), template.getFields());
    assertEquals("ITEM", ((ReportColumn) template.getGridHeaders().get(0)).getLabel());
    assertEquals(1, template.getGridHeaders().size());
    List<?> first = assertInstanceOf(List.class, reportGrid.getData().get(0).get(2));
    assertEquals(1, first.size());
    assertEquals("bolt", value(((List<?>) first.get(0)).get(0), "value"));
    assertEquals("screw", value(((List<?>) assertInstanceOf(List.class, reportGrid.getData().get(1).get(2)).get(0)).get(0), "value"));
  }

  @Test
  void aNestedColumnWithAnUnknownAlignPrintsWithTheDefaultAlign() throws Exception {
    Grid nested = nestedGrid(null);
    nested.getElementsByType(Column.class).forEach(column -> column.setAlign("middle"));
    Grid grid = ordersGrid(g -> g.addElement(new RowDetail().setPrint(true).addElement(nested)));
    when(queryService.launchPrivateQuery(eq("QryLines"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(lines("bolt")));

    ReportGrid reportGrid = design(grid, orders(), false);

    assertNull(((ReportColumn) reportGrid.getRowDetailGrid().getGridHeaders().get(0)).getAlign());
  }

  @Test
  void detailThatTheServerCannotResolveIsSkipped() throws Exception {
    Grid grid = ordersGrid(g -> g.addElement(new RowDetail().setPrint(true).addElement(new Include())));

    ReportGrid reportGrid = design(grid, orders(), false);

    assertNull(reportGrid.getRowDetailField());
    assertEquals(List.of("name", "city"), reportGrid.getFields());
  }

  @Test
  void nestedGridOfAQueryLoadedParentRunsItsQueryPerPrintedRow() throws Exception {
    Grid nested = nestedGrid(null);
    Grid grid = ordersGrid(g -> {
      g.setLoadAll(false);
      g.setTargetAction("QryOrders");
      g.addElement(new RowDetail().setPrint(true).addElement(nested));
    });
    DataList orders = new DataList();
    for (Object[] order : new Object[][]{{7, "Ann", "Oslo"}, {8, "Bob", "Rome"}}) {
      Map<String, CellData> row = new HashMap<>();
      row.put("id", new CellData(order[0]));
      row.put("name", new CellData(order[1]));
      row.put("city", new CellData(order[2]));
      orders.addRow(row);
    }
    when(queryService.launchPrivateQuery(eq("QryOrders"), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(orders));
    when(queryService.launchPrivateQuery(eq("QryLines"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(lines("bolt", "nut")))
      .thenReturn(new ServiceData().setDataList(lines("screw")));

    ReportGrid reportGrid = design(grid, parameters(COLUMNS, data -> {}), false);

    ArgumentCaptor<ObjectNode> query = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService, times(2)).launchPrivateQuery(eq("QryLines"), query.capture());
    assertEquals(7, query.getAllValues().get(0).get("gridId.row.id").asInt());
    assertEquals("Rome", query.getAllValues().get(1).get("gridId.row.city").asString());
    assertEquals("_row_detail_", reportGrid.getRowDetailField());
    List<?> first = assertInstanceOf(List.class, reportGrid.getData().get(0).get(2));
    assertEquals(2, first.size());
    assertEquals("nut", value(((List<?>) first.get(1)).get(0), "value"));
    List<?> second = assertInstanceOf(List.class, reportGrid.getData().get(1).get(2));
    assertEquals("screw", value(((List<?>) second.get(0)).get(0), "value"));
  }
}
