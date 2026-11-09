package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.grid.ReportColumn;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.input.DataBean;
import com.almis.ade.api.enumerate.ChipTone;
import com.almis.ade.api.enumerate.ColumnType;
import com.almis.ade.api.enumerate.SparklineType;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Print design of the cell features of a grid (#929): secondary line, status, chips, progress, sparkline and
 * formatted text
 */
class ReportDesignCellFeaturesTest extends AbstractReportDesignTest {

  @Test
  void secondaryFieldPrintsInTheSameCellOfDocuments() throws Exception {
    ObjectNode parameters = parameters(List.of(
      new PrintColumnData().setName("name").setLabel("Name").setSecondaryField("email").setExportSecondary(false),
      new PrintColumnData().setName("city").setLabel("City")), data -> {
      field(data, "name", cell("Ann", "Ann"));
      field(data, "email", cell("ann@x.com", "ann@x.com"));
      field(data, "city", cell("Oslo", "Oslo"));
    });

    ReportGrid reportGrid = design(grid(grid -> {}, "name", "city"), parameters, false);

    assertEquals(List.of("name", "city", "email"), reportGrid.getFields());
    assertEquals(2, reportGrid.getGridHeaders().size());
    assertEquals("email", reportColumn(reportGrid, 0).getSecondaryField());
    assertEquals("ann@x.com", value(reportGrid.getData().get(0).get(2), "value"));
  }

  @Test
  void secondaryFieldOfANumericColumnFallsBackToItsOwnColumn() throws Exception {
    ObjectNode parameters = parameters(List.of(
      new PrintColumnData().setName("amount").setLabel("Amount").setType("integer").setSecondaryField("note").setSecondaryLabel("Note")),
      data -> {
        field(data, "amount", cell(5, "5"));
        field(data, "note", cell("n", "n"));
      });

    ReportGrid reportGrid = design(grid(grid -> {}, "amount"), parameters, false);

    assertEquals(List.of("amount", "note"), reportGrid.getFields());
    assertEquals(2, reportGrid.getGridHeaders().size());
    assertNull(reportColumn(reportGrid, 0).getSecondaryField());
    assertEquals("Note", reportColumn(reportGrid, 1).getLabel());
  }

  @Test
  void secondaryFieldOfSpreadsheetsOnlyExportsItsColumnWhenAsked() throws Exception {
    ObjectNode parameters = parameters(List.of(
      new PrintColumnData().setName("name").setLabel("Name").setSecondaryField("email").setExportSecondary(false)),
      data -> field(data, "name", cell("Ann", "Ann")));

    ReportGrid reportGrid = design(grid(grid -> {}, "name"), parameters, true);

    assertEquals(List.of("name"), reportGrid.getFields());
    assertNull(reportColumn(reportGrid, 0).getSecondaryField());
  }

  @Test
  void spreadsheetsNeedTheirOwnDesignOnlyWhenAColumnHasASecondaryField() throws Exception {
    Grid grid = grid(g -> {}, "name");
    ObjectNode withSecondary = parameters(List.of(new PrintColumnData().setName("name").setSecondaryField("email")), data -> {});
    ObjectNode plain = parameters(List.of(new PrintColumnData().setName("name")), data -> {});

    assertTrue(reportDesigner.needsSpreadsheetDesign(List.of(grid), withSecondary));
    assertFalse(reportDesigner.needsSpreadsheetDesign(List.of(grid), plain));
  }

  @Test
  void statusFieldPrintsTheCellAsAChipWithTheToneOfTheRow() throws Exception {
    Grid grid = grid(g -> column(g, "state", column -> column.setStatusField("tone")), "state");
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("state").setLabel("State")), data -> {
      field(data, "state", cell("OK", "Done"), cell("LATE", "Late"), cell("X", "Other"));
      field(data, "tone", cell(" Success ", "x"), cell("danger", "x"), cell("weird", "x"));
    });

    ReportGrid reportGrid = design(grid, parameters, false);

    ReportColumn state = reportColumn(reportGrid, 0);
    assertEquals(ColumnType.CHIP, state.getType());
    assertEquals(ChipTone.SUCCESS, state.getChipTones().get("success"));
    assertEquals(ChipTone.DANGER, state.getChipTones().get("danger"));
    assertEquals(List.of("success", "danger", "neutral"), reportGrid.getData().stream()
      .map(row -> (String) value(row.get(0), "value")).toList());
    assertEquals(List.of("Done", "Late", "Other"), reportGrid.getData().stream()
      .map(row -> (String) value(row.get(0), "label")).toList());
  }

  @Test
  void chipsPrintTheItemsOfTheCellSplitBySeparator() throws Exception {
    Grid grid = grid(g -> {
      column(g, "tags", column -> column.setChips(true));
      column(g, "roles", column -> column.setChips(true).setChipSeparator(";").setMaxChips(1));
    }, "tags", "roles");
    ObjectNode parameters = parameters(List.of(
      new PrintColumnData().setName("tags").setLabel("Tags"),
      new PrintColumnData().setName("roles").setLabel("Roles")), data -> {
      field(data, "tags", cell("a, b ,,c", "a, b ,,c"));
      field(data, "roles", cell("x;y", "x;y"));
    });

    ReportGrid reportGrid = design(grid, parameters, false);

    assertEquals(ColumnType.CHIP, reportColumn(reportGrid, 0).getType());
    assertEquals(List.of("a", "b", "c"), chipLabels(reportGrid.getData().get(0).get(0)));
    // The maximum of chips only limits the screen: print shows them all
    assertEquals(List.of("x", "y"), chipLabels(reportGrid.getData().get(0).get(1)));
  }

  @Test
  void progressPrintsTheBarFilledAccordingToTheScaleOfTheColumn() throws Exception {
    Grid grid = grid(g -> column(g, "done", column -> {
      column.setComponentType("progress");
      column.setBarMin(10d).setBarMax(30d);
    }), "done");
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("done").setLabel("Done").setComponent("progress")),
      data -> field(data, "done", cell(20, "20 u"), cell(50, "50 u"), cell(null, "")));

    ReportGrid reportGrid = design(grid, parameters, false);

    assertEquals(ColumnType.PROGRESS, reportColumn(reportGrid, 0).getType());
    // An empty cell (as in the group and total rows) draws no bar
    assertNull(reportGrid.getData().get(2).get(0));
    assertEquals(50d, ((Number) value(reportGrid.getData().get(0).get(0), "value")).doubleValue());
    assertEquals("20 u", value(reportGrid.getData().get(0).get(0), "label"));
    // Over the scale the bar is full
    assertEquals(100d, ((Number) value(reportGrid.getData().get(1).get(0), "value")).doubleValue());
  }

  @Test
  void sparklinePrintsTheSeriesWithItsTypeAndTheDesignSystemColors() throws Exception {
    Grid grid = grid(g -> column(g, "trend", column -> {
      column.setComponentType("sparkline");
      column.setSparklineType("area").setSparklineColor("green").setSparklineNegativeColor("#D64550");
    }), "trend");
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("trend").setLabel("Trend").setComponent("sparkline")),
      data -> field(data, "trend", cell("[1, -2, 3.5]", "[1, -2, 3.5]"), cell("not a series", "not a series")));

    ReportGrid reportGrid = design(grid, parameters, false);

    ReportColumn trend = reportColumn(reportGrid, 0);
    assertEquals(ColumnType.SPARKLINE, trend.getType());
    assertEquals(SparklineType.AREA, trend.getSparklineType());
    // Palette name -> its 500 shade of the AWE 5 tokens; hex as it is
    assertEquals("#22c55e", trend.getSparklineColor());
    assertEquals("#D64550", trend.getSparklineNegativeColor());
    assertEquals(List.of(1d, -2d, 3.5d), value(reportGrid.getData().get(0).get(0), "value"));
    assertEquals(List.of(), value(reportGrid.getData().get(1).get(0), "value"));
  }

  @Test
  void sparklineWithAnUnknownColorOrTypeUsesTheAdeDefaults() throws Exception {
    Grid grid = grid(g -> column(g, "trend", column -> {
      column.setComponentType("sparkline");
      column.setSparklineType("pie").setSparklineColor("var(--x)").setSparklineNegativeColor("#12");
    }), "trend");
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("trend").setLabel("Trend")),
      data -> field(data, "trend", cell("[1]", "[1]")));

    ReportColumn trend = reportColumn(design(grid, parameters, false), 0);

    assertEquals(SparklineType.LINE, trend.getSparklineType());
    assertEquals(new ReportColumn("default").getSparklineColor(), trend.getSparklineColor());
    assertNull(trend.getSparklineNegativeColor());
  }

  @Test
  void formattedTextPrintsAsPlainTextNeverAsMarkup() throws Exception {
    Grid grid = grid(g -> column(g, "notes", column -> column.setComponentType("formatted-text")), "notes");
    String html = "<b>Rock &amp; roll</b><br/>2 &lt; 3<script>alert(1)</script>";
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("notes").setLabel("Notes").setComponent("formatted-text")),
      data -> field(data, "notes", cell(html, html)));

    ReportGrid reportGrid = design(grid, parameters, false);

    assertEquals("Rock & roll 2 < 3", value(reportGrid.getData().get(0).get(0), "label"));
    assertEquals("Rock & roll 2 < 3", value(reportGrid.getData().get(0).get(0), "value"));
  }

  @Test
  void queryGridReadsTheStatusAndSecondaryFieldsFromTheQueryRows() throws Exception {
    Grid grid = grid(g -> {
      g.setLoadAll(false);
      g.setTargetAction("gridQuery");
      column(g, "state", column -> column.setStatusField("tone"));
    }, "state");
    ObjectNode parameters = parameters(List.of(
      new PrintColumnData().setName("state").setLabel("State").setSecondaryField("detail")), data -> {});
    Map<String, CellData> row = new HashMap<>();
    row.put("state", new CellData("OK"));
    row.put("tone", new CellData("warning"));
    row.put("detail", new CellData("since monday"));
    DataList dataList = new DataList();
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(eq("gridQuery"), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));

    ReportGrid reportGrid = design(grid, parameters, false);

    assertEquals(ColumnType.CHIP, reportColumn(reportGrid, 0).getType());
    // A chip cannot hold a second line: the secondary field goes to its own column
    assertEquals(2, reportGrid.getGridHeaders().size());
    assertEquals("detail", reportColumn(reportGrid, 1).getField());
    assertEquals("since monday", value(reportGrid.getData().get(0).get(1), "value"));
    assertEquals("warning", value(reportGrid.getData().get(0).get(0), "value"));
    assertEquals("OK", value(reportGrid.getData().get(0).get(0), "label"));
  }

  @Test
  void gridWithoutCellFeaturesKeepsItsPlainColumns() throws Exception {
    ObjectNode parameters = parameters(List.of(new PrintColumnData().setName("name").setLabel("Name")),
      data -> field(data, "name", cell("Ann", "Ann")));

    ReportGrid reportGrid = design(grid(grid -> {}, "name"), parameters, false);

    assertEquals(List.of("name"), reportGrid.getFields());
    ReportColumn name = reportColumn(reportGrid, 0);
    assertEquals(ColumnType.STRING, name.getType());
    assertNull(name.getSecondaryField());
    assertEquals("Ann", value(reportGrid.getData().get(0).get(0), "value"));
    assertEquals(0, reportGrid.getRowStyles().size());
  }

  private static List<String> chipLabels(Object cell) {
    Collection<?> chips = assertInstanceOf(Collection.class, cell);
    return chips.stream().map(chip -> (String) ((DataBean) chip).getSingleValue("label")).toList();
  }
}
