package com.almis.awe.service.report;

import com.almis.ade.api.bean.component.Layout;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportDesignTest {

  @InjectMocks
  private ReportDesigner reportDesigner;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private ObjectMapper mapper;

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
}
