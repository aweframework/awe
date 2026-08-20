package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.Image;
import com.almis.ade.api.bean.component.Layout;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.ade.api.enumerate.HorizontalTextAlignment;
import com.almis.awe.builder.screen.chart.ChartBuilder;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
}
