package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.grid.ReportColumn;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.style.AdeStyle;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Print design of the grouped grids (#929): the groups the user has chosen at runtime, with group rows, subtotals and the
 * grand total
 */
class ReportDesignGroupingTest extends AbstractReportDesignTest {

  private static final String SUBTOTAL = "GRID_GROUP_SUBTOTAL";
  private static final String INDENT = "  ";
  private static final List<PrintColumnData> COLUMNS = List.of(
    new PrintColumnData().setName("region").setLabel("Region"),
    new PrintColumnData().setName("city").setLabel("City"),
    new PrintColumnData().setName("amount").setLabel("Amount").setType("integer"));

  /** Four rows: N/A 10, N/A 5, N/B 7 and S/C 3 (25 in all), with the grand total the client sends */
  private ObjectNode sales(List<PrintColumnData> visible, ArrayNode groupBy) {
    return parameters(visible, data -> {
      field(data, "region", cell("N", "N"), cell("N", "N"), cell("N", "N"), cell("S", "S"));
      field(data, "city", cell("A", "A"), cell("A", "A"), cell("B", "B"), cell("C", "C"));
      field(data, "amount", cell(10, "10"), cell(5, "5"), cell(7, "7"), cell(3, "3"));
      ObjectNode gridData = (ObjectNode) data.get(GRID_ID + ".data");
      gridData.set("footer", JsonNodeFactory.instance.objectNode().set("amount", cell(25, "25")));
      if (groupBy != null) {
        gridData.set("groupBy", groupBy);
      }
    });
  }

  private static ArrayNode levels(String... names) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    for (String name : names) {
      array.add(name);
    }
    return array;
  }

  private Grid salesGrid(Consumer<Grid> customizer) throws Exception {
    return grid(grid -> {
      column(grid, "amount", column -> column.setSummaryType("sum"));
      // The user can group: the grid has the group selector
      grid.setGroupSelector(true);
      grid.setShowTotals(true);
      customizer.accept(grid);
    }, "region", "city", "amount");
  }

  /** Every row as style|first column|amount, the way the report prints it */
  private static List<String> rows(ReportGrid reportGrid) {
    int styleIndex = reportGrid.getFields().indexOf("_style_");
    int amountIndex = reportGrid.getFields().indexOf("amount");
    return reportGrid.getData().stream().map(row -> text(styleIndex < 0 ? null : value(row.get(styleIndex), "value"))
      + "|" + text(value(row.get(0), "value")) + "|" + text(value(row.get(amountIndex), "value"))).toList();
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString();
  }

  @Test
  void rowsGroupByTheLevelsTheUserChoseWithSubtotalsAndTheGrandTotal() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(COLUMNS, levels("region", "city")), false);

    assertEquals(List.of(
      "GROUP1|N (3)|22",
      "GROUP2|" + INDENT + "A (2)|15", "|N|10", "|N|5", "SUBTOTAL|" + INDENT + SUBTOTAL + " A|15",
      "GROUP2|" + INDENT + "B (1)|7", "|N|7", "SUBTOTAL|" + INDENT + SUBTOTAL + " B|7",
      "SUBTOTAL|" + SUBTOTAL + " N|22",
      "GROUP1|S (1)|3",
      "GROUP2|" + INDENT + "C (1)|3", "|S|3", "SUBTOTAL|" + INDENT + SUBTOTAL + " C|3",
      "SUBTOTAL|" + SUBTOTAL + " S|3",
      "TOTAL||25"), rows(reportGrid));
  }

  @Test
  void theStyleColumnOfAGroupedGridIsNotPrinted() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(COLUMNS, levels("region")), false);

    assertEquals("_style_", reportGrid.getStyleColumn().getField());
    assertTrue(reportGrid.getGridHeaders().stream()
      .noneMatch(header -> header instanceof ReportColumn column && "_style_".equals(column.getField())));
  }

  @Test
  void theGroupAndTotalRowsLeaveADrawnCellWithoutValueBlank() throws Exception {
    Grid grid = grid(g -> {
      column(g, "amount", column -> column.setSummaryType("sum"));
      column(g, "done", column -> column.setComponentType("progress"));
      g.setGroupSelector(true);
      g.setShowTotals(true);
    }, "region", "done", "amount");
    List<PrintColumnData> visible = List.of(
      new PrintColumnData().setName("region").setLabel("Region"),
      new PrintColumnData().setName("done").setLabel("Done").setComponent("progress"),
      new PrintColumnData().setName("amount").setLabel("Amount").setType("integer"));
    ObjectNode parameters = parameters(visible, data -> {
      field(data, "region", cell("N", "N"), cell("S", "S"));
      field(data, "done", cell(40, "40"), cell(60, "60"));
      field(data, "amount", cell(10, "10"), cell(3, "3"));
      ObjectNode gridData = (ObjectNode) data.get(GRID_ID + ".data");
      gridData.set("footer", JsonNodeFactory.instance.objectNode().set("amount", cell(13, "13")));
      gridData.set("groupBy", levels("region"));
    });

    ReportGrid reportGrid = design(grid, parameters, false);

    int done = reportGrid.getFields().indexOf("done");
    // Group N, its row, its subtotal, group S, its row, its subtotal and the total
    List<Object> cells = reportGrid.getData().stream().map(row -> row.get(done)).toList();
    assertEquals(7, cells.size());
    assertNull(cells.get(0));
    assertEquals(40d, ((Number) value(cells.get(1), "value")).doubleValue());
    assertNull(cells.get(2));
    assertNull(cells.get(6));
  }

  @Test
  void theSubtotalsOfTheFirstLevelAddUpToTheGrandTotal() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(COLUMNS, levels("region", "city")), false);

    int firstLevelSubtotals = rows(reportGrid).stream()
      .filter(row -> row.startsWith("SUBTOTAL|" + SUBTOTAL))
      .mapToInt(row -> Integer.parseInt(row.substring(row.lastIndexOf('|') + 1))).sum();

    assertEquals(25, firstLevelSubtotals);
  }

  @Test
  void groupsWithoutSubtotalsWhenTheGridTurnsThemOff() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> grid.setGroupSubtotals(false)), sales(COLUMNS, levels("region")), false);

    assertEquals(List.of("GROUP1|N (3)|22", "|N|10", "|N|5", "|N|7", "GROUP1|S (1)|3", "|S|3", "TOTAL||25"), rows(reportGrid));
  }

  @Test
  void groupRowsGetABoldStyleOnTheBackgroundOfTheDesignSystemSurfaceTokens() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(COLUMNS, levels("region", "city")), false);

    AdeStyle first = reportGrid.getRowStyles().get("GROUP1");
    assertTrue(first.getBold());
    assertEquals(Color.decode("#e2e8f0"), first.getBackgroundColor());
    assertEquals(Color.decode("#f1f5f9"), reportGrid.getRowStyles().get("GROUP2").getBackgroundColor());
    assertEquals(2, reportGrid.getRowStyles().size());
  }

  @Test
  void theGroupByOfTheGridApplieswhenTheClientSendsNoLevels() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> grid.setGroupBy("region")), sales(COLUMNS, null), false);

    assertEquals("GROUP1|N (3)|22", rows(reportGrid).get(0));
  }

  @Test
  void anEmptyListOfLevelsMeansTheUserUngroupedTheGrid() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> grid.setGroupBy("region")), sales(COLUMNS, levels()), false);

    assertEquals(List.of("|N|10", "|N|5", "|N|7", "|S|3", "TOTAL||25"), rows(reportGrid));
    assertEquals(0, reportGrid.getRowStyles().size());
  }

  @Test
  void aGridThatCannotGroupIgnoresTheLevelsAndUnknownColumnsAreSkipped() throws Exception {
    ReportGrid treeGrid = design(salesGrid(grid -> grid.setGroupBy("region").setTreegrid(true)), sales(COLUMNS, levels("region")), false);
    ReportGrid unknown = design(salesGrid(grid -> grid.setGroupSelector(true)), sales(COLUMNS, levels("nothing", "region")), false);

    assertEquals("|N|10", rows(treeGrid).get(0));
    assertEquals("GROUP1|N (3)|22", rows(unknown).get(0));
    assertNull(unknown.getRowStyles().get("GROUP2"));
  }

  @Test
  void groupsOfALevelThatMixesNumbersAndTextsPutTheNumbersFirst() throws Exception {
    ObjectNode parameters = parameters(COLUMNS, data -> {
      // Comparing numbers as numbers and texts as texts is not transitive here: it sorted them 10, 5a, 9
      field(data, "region", cell("9", "9"), cell("5a", "5a"), cell("10", "10"));
      field(data, "city", cell("A", "A"), cell("B", "B"), cell("C", "C"));
      field(data, "amount", cell(3, "3"), cell(1, "1"), cell(2, "2"));
      ((ObjectNode) data.get(GRID_ID + ".data")).set("groupBy", levels("region"));
    });

    ReportGrid reportGrid = design(salesGrid(grid -> grid.setGroupSubtotals(false).setShowTotals(false)), parameters, false);

    assertEquals(List.of("GROUP1|9 (1)|3", "|9|3", "GROUP1|10 (1)|2", "|10|2", "GROUP1|5a (1)|1", "|5a|1"),
      rows(reportGrid));
  }

  @Test
  void groupsThatDifferOnlyInCaseKeepTheSameOrderWhateverOrderTheRowsComeIn() throws Exception {
    ObjectNode parameters = parameters(COLUMNS, data -> {
      field(data, "region", cell("b", "b"), cell("B", "B"), cell("a", "a"));
      field(data, "city", cell("A", "A"), cell("B", "B"), cell("C", "C"));
      field(data, "amount", cell(1, "1"), cell(2, "2"), cell(3, "3"));
      ((ObjectNode) data.get(GRID_ID + ".data")).set("groupBy", levels("region"));
    });

    ReportGrid reportGrid = design(salesGrid(grid -> grid.setGroupSubtotals(false).setShowTotals(false)), parameters, false);

    assertEquals(List.of("GROUP1|a (1)|3", "|a|3", "GROUP1|B (1)|2", "|B|2", "GROUP1|b (1)|1", "|b|1"), rows(reportGrid));
  }

  @Test
  void theGroupLabelGoesInTheFirstTextColumnWhenTheFirstPrintedColumnIsANumber() throws Exception {
    List<PrintColumnData> numberFirst = List.of(COLUMNS.get(2), COLUMNS.get(0), COLUMNS.get(1));

    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(numberFirst, levels("region")), false);

    List<Object> group = reportGrid.getData().get(0);
    assertEquals("N (3)", value(group.get(reportGrid.getFields().indexOf("region")), "value"));
    assertEquals(22, ((Number) value(group.get(reportGrid.getFields().indexOf("amount")), "value")).intValue());
  }

  @Test
  void aGroupedGridWithoutTextColumnsPrintsItsGroupsWithoutALabel() throws Exception {
    ReportGrid reportGrid = design(salesGrid(grid -> {}), sales(List.of(COLUMNS.get(2)), levels("region")), false);

    // The only printed column is the amount: group and subtotal rows show its aggregate, never the label
    assertEquals(List.of("GROUP1|22|22", "|10|10", "|5|5", "|7|7", "SUBTOTAL|22|22", "GROUP1|3|3", "|3|3", "SUBTOTAL|3|3",
      "TOTAL|25|25"), rows(reportGrid));
  }

  @Test
  void rowsGroupByAColumnThatIsNotPrintedAndAnEmptyValueGetsTheEmptyLabel() throws Exception {
    ObjectNode parameters = parameters(List.of(COLUMNS.get(1), COLUMNS.get(2)), data -> {
      field(data, "city", cell("A", "A"), cell("B", "B"));
      field(data, "amount", cell(10, "10"), cell(5, "5"));
      field(data, "region", cell("N", "N"), cell(null, ""));
      ((ObjectNode) data.get(GRID_ID + ".data")).set("groupBy", levels("region"));
      ((ObjectNode) data.get(GRID_ID + ".data")).set("footer", JsonNodeFactory.instance.objectNode().set("amount", cell(15, "15")));
    });

    ReportGrid reportGrid = design(salesGrid(grid -> {}), parameters, false);

    assertEquals(List.of("GROUP1|N (1)|10", "|A|10", "SUBTOTAL|" + SUBTOTAL + " N|10",
      "GROUP1|GRID_GROUP_EMPTY (1)|5", "|B|5", "SUBTOTAL|" + SUBTOTAL + " GRID_GROUP_EMPTY|5", "TOTAL||15"), rows(reportGrid));
  }
}
