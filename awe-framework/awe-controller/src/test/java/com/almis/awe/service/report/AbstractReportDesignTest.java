package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.Layout;
import com.almis.ade.api.bean.component.grid.ReportColumn;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.input.DataBean;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Shared fixture of the report design tests: a designer with a real object mapper and helpers to design one grid
 * from its column definitions and the data the client sends
 */
abstract class AbstractReportDesignTest {

  static final String GRID_ID = "gridId";

  protected final BaseConfigProperties baseConfigProperties = new BaseConfigProperties();
  protected final QueryService queryService = mock(QueryService.class);
  protected final ObjectMapper mapper = new ObjectMapper();
  // The locale texts of the design are returned as their keys
  protected final ReportDesigner reportDesigner = spy(new ReportDesigner(queryService, mapper, baseConfigProperties));

  @BeforeEach
  void stubLocale() {
    doAnswer(invocation -> invocation.getArgument(0)).when(reportDesigner).getLocale(anyString());
  }

  /**
   * A cell as the clients send it
   */
  protected static ObjectNode cell(Object value, String label) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    if (value instanceof Integer number) {
      node.put("value", number.intValue());
    } else if (value instanceof Number number) {
      node.put("value", number.doubleValue());
    } else if (value != null) {
      node.put("value", value.toString());
    } else {
      node.putNull("value");
    }
    return node.put("label", label);
  }

  /**
   * Column data of the grid: one cell per row
   */
  protected static ArrayNode column(ObjectNode... cells) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    Arrays.stream(cells).forEach(array::add);
    return array;
  }

  /**
   * Print parameters of a grid that loads all its data: visible columns, sent data of every field and its values
   */
  protected ObjectNode parameters(List<PrintColumnData> visibleColumns, Consumer<ObjectNode> data) {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", mapper.valueToTree(visibleColumns));
    parameters.set(GRID_ID + ".data", gridData);
    data.accept(parameters);
    return parameters;
  }

  /**
   * Send a field the way the clients do: its plain values and its data (value and label)
   */
  protected static void field(ObjectNode parameters, String name, ObjectNode... cells) {
    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    Arrays.stream(cells).forEach(cell -> values.add(cell.get("value")));
    parameters.set(name, values);
    parameters.set(name + ".data", column(cells));
  }

  /**
   * Grid definition with the given columns, each one customized by the consumer
   */
  protected static Grid grid(Consumer<Grid> customizer, String... columns) throws Exception {
    GridBuilder builder = new GridBuilder().setId(GRID_ID).setLoadAll(true);
    Arrays.stream(columns).forEach(name -> builder.addColumn(new TextColumnBuilder().setName(name)));
    Grid grid = builder.build();
    // Plain columns: no component, as in a screen definition
    grid.getElementsByType(Column.class).forEach(column -> column.setComponentType(null));
    customizer.accept(grid);
    return grid;
  }

  /**
   * Set the attributes of a column of the grid
   */
  protected static void column(Grid grid, String name, Consumer<Column> customizer) {
    grid.getElementsByType(Column.class).stream()
      .filter(column -> name.equals(column.getName()))
      .forEach(customizer);
  }

  /**
   * Design the grid and retrieve its report grid
   */
  protected ReportGrid design(Grid grid, ObjectNode parameters, boolean spreadsheet) throws Exception {
    PrintBean printBean = reportDesigner.getPrintDesign(Collections.singletonList(grid), parameters, spreadsheet);
    return (ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0);
  }

  protected static ReportColumn reportColumn(ReportGrid reportGrid, int index) {
    return (ReportColumn) reportGrid.getGridHeaders().get(index);
  }

  protected static Object value(Object cell, String key) {
    return ((DataBean) cell).getSingleValue(key);
  }
}
