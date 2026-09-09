package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.Criterion;
import com.almis.ade.api.bean.component.Image;
import com.almis.ade.api.bean.component.Layout;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.component.grid.ReportHeader;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.ade.api.enumerate.HorizontalTextAlignment;
import com.almis.awe.builder.screen.chart.ChartBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.QueryService;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.entities.Element;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDesignTest {

  @Spy
  @InjectMocks
  private ReportDesigner reportDesigner;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private ObjectMapper mapper;

  @Mock
  private QueryService queryService;

  @Test
  void getPrintDesign() throws Exception {

    List<PrintColumnData> columnDataList = Arrays.asList(
            new PrintColumnData().setName("intColumn").setLabel("intColumn"),
            new PrintColumnData().setName("longColumn").setLabel("longColumn"),
            new PrintColumnData().setName("floatColumn").setLabel("floatColumn"),
            new PrintColumnData().setName("booleanColumn").setLabel("booleanColumn"),
            new PrintColumnData().setName("bigDecimalColumn").setLabel("bigDecimalColumn"),
            new PrintColumnData().setName("bigIntegerColumn").setLabel("bigIntegerColumn")
    );

    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", mapper.valueToTree(columnDataList));

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);
    parameters.set("intColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1)));
    parameters.set("longColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1L)));
    parameters.set("floatColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 2F)));
    parameters.set("booleanColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", true)));
    parameters.set("bigDecimalColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", new BigDecimal("121.3"))));
    parameters.set("bigIntegerColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", new BigInteger("121"))));
    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setLoadAll(true)
        .addColumn(new TextColumnBuilder().setName("intColumn"))
        .addColumn(new TextColumnBuilder().setName("longColumn"))
        .addColumn(new TextColumnBuilder().setName("floatColumn"))
        .addColumn(new TextColumnBuilder().setName("booleanColumn"))
        .addColumn(new TextColumnBuilder().setName("bigDecimalColumn"))
        .addColumn(new TextColumnBuilder().setName("bigIntegerColumn"))
        .build()
    );
    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);
    assertEquals(6, ((ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0)).getGridHeaders().size());
  }

  @Test
  void getPrintDesignWithQueryGridCoversEveryCellType() throws Exception {
    List<PrintColumnData> columnDataList = Arrays.asList(
      new PrintColumnData().setName("objectColumn").setLabel("objectColumn"),
      new PrintColumnData().setName("nullColumn").setLabel("nullColumn"),
      new PrintColumnData().setName("floatColumn").setLabel("floatColumn"),
      new PrintColumnData().setName("intColumn").setLabel("intColumn"),
      new PrintColumnData().setName("stringColumn").setLabel("stringColumn")
    );
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    when(mapper.convertValue(any(ObjectNode.class), any(TypeReference.class)))
      .thenReturn(new HashMap<>(Map.of("value", "converted")));

    // One row per cell type the row-data switch handles
    Map<String, CellData> row = new HashMap<>();
    row.put("objectColumn", new CellData().setValue(JsonNodeFactory.instance.objectNode().put("value", "x")));
    row.put("nullColumn", new CellData());
    row.put("floatColumn", new CellData(2.5F));
    row.put("intColumn", new CellData(3));
    row.put("stringColumn", new CellData("text"));
    DataList dataList = new DataList();
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(eq("gridQuery"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(dataList));

    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setTargetAction("gridQuery")
        .addColumn(new TextColumnBuilder().setName("objectColumn"))
        .addColumn(new TextColumnBuilder().setName("nullColumn"))
        .addColumn(new TextColumnBuilder().setName("floatColumn"))
        .addColumn(new TextColumnBuilder().setName("intColumn"))
        .addColumn(new TextColumnBuilder().setName("stringColumn"))
        .build()
    );

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    ReportGrid reportGrid = (ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0);
    assertEquals(1, reportGrid.getData().size());
    assertEquals(5, reportGrid.getData().get(0).size());
  }

  @Test
  void getPrintDesignWithPaginatedQueryGridLiftsGridPaginationToTheQuery() throws Exception {
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(Collections.emptyList());
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    when(queryService.launchPrivateQuery(eq("gridQuery"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    // The client nests the grid pagination under <gridId>.data (see grid/base.js getSpecificFields)
    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());
    gridData.put("max", 10);
    gridData.put("page", 3);
    gridData.set("sort", JsonNodeFactory.instance.arrayNode()
      .add(JsonNodeFactory.instance.objectNode().put("id", "name").put("direction", "asc")));

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setTargetAction("gridQuery")
        .addColumn(new TextColumnBuilder().setName("name"))
        .build()
    );

    reportDesigner.getPrintDesign(reportElementList, parameters);

    ArgumentCaptor<ObjectNode> queryParameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("gridQuery"), queryParameters.capture());
    ObjectNode rootParameters = queryParameters.getValue();
    assertEquals(10, rootParameters.get("max").asInt(), "grid page size must reach the query root");
    assertEquals(3, rootParameters.get("page").asInt(), "grid current page must reach the query root");
    assertEquals("name", rootParameters.get("sort").get(0).get("id").asText());
  }

  @Test
  void getPrintDesignWithQueryGridWithoutClientPaginationLeavesQueryDefaults() throws Exception {
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(Collections.emptyList());
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    when(queryService.launchPrivateQuery(eq("gridQuery"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));

    // No max/page/sort sent by the client: nothing must be forced (a null max would disable pagination)
    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setTargetAction("gridQuery")
        .addColumn(new TextColumnBuilder().setName("name"))
        .build()
    );

    reportDesigner.getPrintDesign(reportElementList, parameters);

    ArgumentCaptor<ObjectNode> queryParameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("gridQuery"), queryParameters.capture());
    ObjectNode rootParameters = queryParameters.getValue();
    assertFalse(rootParameters.has("max"), "max must not be forced when the client did not send it");
    assertFalse(rootParameters.has("page"), "page must not be forced when the client did not send it");
    assertFalse(rootParameters.has("sort"), "sort must not be forced when the client did not send it");
  }

  @Test
  void getPrintDesignWithLabelledGridSetsStyledTitle() throws Exception {
    List<PrintColumnData> columnDataList = Collections.singletonList(
      new PrintColumnData().setName("intColumn").setLabel("intColumn"));
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    doReturn("Grid title").when(reportDesigner).getLocale(anyString());

    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);
    parameters.set("intColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1)));

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setLabel("GRID_LABEL")
        .setLoadAll(true)
        .addColumn(new TextColumnBuilder().setName("intColumn"))
        .build()
    );

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    ReportGrid reportGrid = (ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0);
    assertNotNull(reportGrid.getTitle());
    assertEquals("Grid title", reportGrid.getTitle().getValue());
    assertNotNull(reportGrid.getTitle().getStyle());
  }

  @Test
  void getPrintDesignWithCriterionAddsTitleAndValue() throws Exception {
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    doReturn("Criterion label").when(reportDesigner).getLocale("CRITERION_LABEL");

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("criterionId.data", JsonNodeFactory.instance.objectNode().put("text", "criterion value"));

    List<Element> reportElementList = Collections.singletonList(
      new TextCriteriaBuilder().setId("criterionId").setLabel("CRITERION_LABEL").build());

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    Layout criteriaLayout = (Layout) ((Layout) printBean.getDetail()).getElements().get(0);
    Criterion criterionElement = assertInstanceOf(Criterion.class, criteriaLayout.getElements().get(0));
    assertEquals("Criterion label", criterionElement.getTitle());
    assertEquals("criterion value", criterionElement.getValue());
  }

  @Test
  void getPrintDesignWithCriterionDataOverwrittenByAGridColumnSkipsTheCriterion() throws Exception {
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("criterionId.data", JsonNodeFactory.instance.arrayNode()
      .add(JsonNodeFactory.instance.objectNode().put("value", 1).put("label", "one")));

    List<Element> reportElementList = Collections.singletonList(
      new TextCriteriaBuilder().setId("criterionId").setLabel("CRITERION_LABEL").build());

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    Layout criteriaLayout = (Layout) ((Layout) printBean.getDetail()).getElements().get(0);
    assertTrue(criteriaLayout.getElements().isEmpty());
  }

  @Test
  void getPrintDesignWithCriterionDataWithoutTextFieldSkipsTheCriterion() throws Exception {
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("criterionId.data", JsonNodeFactory.instance.objectNode().put("value", "no text field"));

    List<Element> reportElementList = Collections.singletonList(
      new TextCriteriaBuilder().setId("criterionId").setLabel("CRITERION_LABEL").build());

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    Layout criteriaLayout = (Layout) ((Layout) printBean.getDetail()).getElements().get(0);
    assertTrue(criteriaLayout.getElements().isEmpty());
  }

  @Test
  void getPrintDesignWithChartCentersSvgImage() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("chartId", JsonNodeFactory.instance.objectNode().put("image", "<svg/>"));

    List<Element> reportElementList = new ArrayList<>();
    reportElementList.add(new ChartBuilder().setId("chartId").build());

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);

    Layout chartLayout = (Layout) ((Layout) printBean.getDetail()).getElements().get(0);
    Image chartElement = assertInstanceOf(Image.class, chartLayout.getElements().get(0));
    assertEquals("<svg/>", chartElement.getSVGImage());
    assertEquals(HorizontalTextAlignment.CENTER, chartElement.getStyle().getHorizontalTextAlignment());
  }

  /**
   * Build a grid design with columns declared as spreadsheet only (issue #753)
   */
  private ReportGrid designGridWithSpreadsheetOnlyColumns(boolean includeSpreadsheetOnlyColumns) throws Exception {
    List<PrintColumnData> columnDataList = Arrays.asList(
      new PrintColumnData().setName("visibleColumn").setLabel("visibleColumn"),
      new PrintColumnData().setName("excelColumn").setLabel("excelColumn").setPrintable("excel"),
      new PrintColumnData().setName("Mixed header").setLabel("Mixed header").setHeader(true).setColumnList(Arrays.asList(
        new PrintColumnData().setName("excelInHeader").setLabel("excelInHeader").setPrintable("EXCEL"),
        new PrintColumnData().setName("alwaysInHeader").setLabel("alwaysInHeader").setPrintable("true"))),
      new PrintColumnData().setName("Excel header").setLabel("Excel header").setHeader(true).setColumnList(Collections.singletonList(
        new PrintColumnData().setName("onlyExcelInHeader").setLabel("onlyExcelInHeader").setPrintable("excel")))
    );
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);
    for (String field : Arrays.asList("visibleColumn", "excelColumn", "excelInHeader", "alwaysInHeader", "onlyExcelInHeader")) {
      parameters.set(field, JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1)));
    }

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder()
        .setId("gridId")
        .setLoadAll(true)
        .addColumn(new TextColumnBuilder().setName("visibleColumn"))
        .addColumn(new TextColumnBuilder().setName("excelColumn"))
        .addColumn(new TextColumnBuilder().setName("excelInHeader"))
        .addColumn(new TextColumnBuilder().setName("alwaysInHeader"))
        .addColumn(new TextColumnBuilder().setName("onlyExcelInHeader"))
        .build()
    );

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters, includeSpreadsheetOnlyColumns);
    return (ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0);
  }

  @Test
  void getPrintDesignForDocumentOutputsSkipsSpreadsheetOnlyColumns() throws Exception {
    ReportGrid reportGrid = designGridWithSpreadsheetOnlyColumns(false);

    assertEquals(Arrays.asList("visibleColumn", "alwaysInHeader"), reportGrid.getFields());
    assertEquals(2, reportGrid.getGridHeaders().size());
    ReportHeader header = assertInstanceOf(ReportHeader.class, reportGrid.getGridHeaders().get(1));
    assertEquals("Mixed header", header.getLabel());
    assertEquals(1, header.getColumns().size());
  }

  @Test
  void getPrintDesignForSpreadsheetOutputsKeepsSpreadsheetOnlyColumns() throws Exception {
    ReportGrid reportGrid = designGridWithSpreadsheetOnlyColumns(true);

    assertEquals(Arrays.asList("visibleColumn", "excelColumn", "excelInHeader", "alwaysInHeader", "onlyExcelInHeader"), reportGrid.getFields());
    assertEquals(4, reportGrid.getGridHeaders().size());
    ReportHeader header = assertInstanceOf(ReportHeader.class, reportGrid.getGridHeaders().get(2));
    assertEquals(2, header.getColumns().size());
  }

  @Test
  void getPrintDesignWithoutProfileBehavesAsDocumentOutput() throws Exception {
    List<PrintColumnData> columnDataList = Arrays.asList(
      new PrintColumnData().setName("intColumn").setLabel("intColumn"),
      new PrintColumnData().setName("excelColumn").setLabel("excelColumn").setPrintable("excel"));
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode gridData = JsonNodeFactory.instance.objectNode();
    gridData.set("visibleColumns", JsonNodeFactory.instance.arrayNode());
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", gridData);
    parameters.set("intColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1)));
    parameters.set("excelColumn", JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("value", 1)));

    List<Element> reportElementList = Collections.singletonList(
      new GridBuilder().setId("gridId").setLoadAll(true)
        .addColumn(new TextColumnBuilder().setName("intColumn"))
        .addColumn(new TextColumnBuilder().setName("excelColumn"))
        .build());

    PrintBean printBean = reportDesigner.getPrintDesign(reportElementList, parameters);
    ReportGrid reportGrid = (ReportGrid) ((Layout) ((Layout) printBean.getDetail()).getElements().get(0)).getElements().get(0);
    assertEquals(Collections.singletonList("intColumn"), reportGrid.getFields());
  }

  @Test
  void hasSpreadsheetOnlyColumnsDetectsColumnsInsideHeaders() throws Exception {
    List<PrintColumnData> columnDataList = Arrays.asList(
      new PrintColumnData().setName("intColumn").setLabel("intColumn"),
      new PrintColumnData().setName("Header").setLabel("Header").setHeader(true).setColumnList(Collections.singletonList(
        new PrintColumnData().setName("excelColumn").setLabel("excelColumn").setPrintable("excel"))));
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", JsonNodeFactory.instance.objectNode().set("visibleColumns", JsonNodeFactory.instance.arrayNode()));
    List<Element> reportElementList = Arrays.asList(
      new TextCriteriaBuilder().setId("criterion").build(),
      new GridBuilder().setId("gridId").build());

    assertTrue(reportDesigner.hasSpreadsheetOnlyColumns(reportElementList, parameters));
  }

  @Test
  void hasSpreadsheetOnlyColumnsIsFalseWithoutExcelColumnsOrWithoutGridData() throws Exception {
    List<PrintColumnData> columnDataList = Collections.singletonList(
      new PrintColumnData().setName("intColumn").setLabel("intColumn").setPrintable("true"));
    when(mapper.readValue(any(JsonParser.class), any(TypeReference.class))).thenReturn(columnDataList);
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.set("gridId.data", JsonNodeFactory.instance.objectNode().set("visibleColumns", JsonNodeFactory.instance.arrayNode()));
    List<Element> reportElementList = Arrays.asList(
      new GridBuilder().setId("gridId").build(),
      new GridBuilder().setId("gridWithoutData").build());

    assertFalse(reportDesigner.hasSpreadsheetOnlyColumns(reportElementList, parameters));
  }

  @Test
  void isSpreadsheetFormatOnlyMatchesSpreadsheetOutputs() {
    assertTrue(ReportDesigner.isSpreadsheetFormat("XLSX"));
    assertTrue(ReportDesigner.isSpreadsheetFormat("xlsx"));
    assertTrue(ReportDesigner.isSpreadsheetFormat("CSV"));
    assertFalse(ReportDesigner.isSpreadsheetFormat("PDF"));
    assertFalse(ReportDesigner.isSpreadsheetFormat("DOCX"));
    assertFalse(ReportDesigner.isSpreadsheetFormat("TEXT"));
    assertFalse(ReportDesigner.isSpreadsheetFormat("unknown"));
    assertFalse(ReportDesigner.isSpreadsheetFormat(null));
  }
}
