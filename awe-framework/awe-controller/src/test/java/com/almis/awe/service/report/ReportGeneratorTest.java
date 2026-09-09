package com.almis.awe.service.report;

import com.almis.ade.api.ADE;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.ade.api.fluid.engine.generic.TemplateExporterBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorTest {

  @Spy
  @InjectMocks
  private ReportGenerator reportGenerator;

  @Mock
  private ReportDesigner designer;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ADE adeAPI;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private Screen screen;

  private final List<Element> reportStructure = Collections.emptyList();
  private ObjectNode parameters;

  @BeforeEach
  void setUp() throws Exception {
    parameters = JsonNodeFactory.instance.objectNode();
    when(baseConfigProperties.getComponent()).thenReturn(new BaseConfigProperties.Component());
    when(baseConfigProperties.getPaths()).thenReturn(new BaseConfigProperties.Paths());
    when(screen.getReportStructure(any(), any(), any(), any())).thenReturn(reportStructure);
    when(designer.getPrintDesign(any(), any(), anyBoolean())).thenReturn(new PrintBean());

    doReturn(parameters).when(reportGenerator).getMutableRequestParameters();
    doReturn("Screen title").when(reportGenerator).getLocale(any());
    doNothing().when(reportGenerator).putRequestParameter(any(ObjectNode.class), anyString(), anyString());
    doNothing().when(reportGenerator).mergePropagatedRequestParameters(any());
    doReturn(new FileData()).when(reportGenerator).generateReportFormat(any(TemplateExporterBuilder.class), anyString(), anyString(), anyString(), any(ObjectNode.class));
  }

  @Test
  void generateScreenReportContextSharesOneDesignWhenNoColumnIsSpreadsheetOnly() throws Exception {
    parameters.set(AweConstants.PRINT_FORMATS, JsonNodeFactory.instance.arrayNode().add("PDF").add("XLSX").add("CSV"));
    when(designer.hasSpreadsheetOnlyColumns(reportStructure, parameters)).thenReturn(false);

    ReportGenerator.GeneratedScreenReportContext context = reportGenerator.generateScreenReportContext(screen);

    assertEquals(3, context.reportFiles().size());
    verify(designer, times(1)).getPrintDesign(reportStructure, parameters, false);
    verify(designer, never()).getPrintDesign(any(), any(), eq(true));
  }

  @Test
  void generateScreenReportContextDesignsSpreadsheetOutputsApartWhenSomeColumnIsSpreadsheetOnly() throws Exception {
    parameters.set(AweConstants.PRINT_FORMATS, JsonNodeFactory.instance.arrayNode().add("PDF").add("XLSX").add("CSV").add("DOCX"));
    when(designer.hasSpreadsheetOnlyColumns(reportStructure, parameters)).thenReturn(true);

    ReportGenerator.GeneratedScreenReportContext context = reportGenerator.generateScreenReportContext(screen);

    assertEquals(4, context.reportFiles().size());
    verify(designer, times(1)).getPrintDesign(reportStructure, parameters, false);
    verify(designer, times(1)).getPrintDesign(reportStructure, parameters, true);
  }

  @Test
  void generateScreenReportContextWithOnlyDocumentOutputsNeverDesignsForSpreadsheets() throws Exception {
    parameters.set(AweConstants.PRINT_FORMATS, JsonNodeFactory.instance.arrayNode().add("PDF"));
    when(designer.hasSpreadsheetOnlyColumns(reportStructure, parameters)).thenReturn(true);

    reportGenerator.generateScreenReportContext(screen);

    verify(designer, times(1)).getPrintDesign(reportStructure, parameters, false);
    verify(designer, never()).getPrintDesign(any(), any(), eq(true));
  }
}
