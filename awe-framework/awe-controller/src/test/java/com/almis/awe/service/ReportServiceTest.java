package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.report.ReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @InjectMocks
  ReportService reportService;

  @Mock
  ReportGenerator reportGenerator;

  @Mock
  MaintainService maintainService;

  @Mock
  MenuService menuService;

  @TempDir
  static File tempFolder;

  @Test
  void printScreen() throws Exception {
    // Mock
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    ReportGenerator.GeneratedScreenReportContext reportContext = new ReportGenerator.GeneratedScreenReportContext(Arrays.asList(), parameters);
    when(reportGenerator.generateScreenReportContext(any())).thenReturn(reportContext);
    when(reportGenerator.downloadScreenReportFiles(anyList())).thenReturn(new ServiceData().setClientActionList(Arrays.asList(new ClientAction(), new ClientAction())));

    // Test
    ServiceData serviceData = reportService.printScreen("screen", "CREATE");

    // Verify
    verify(menuService, times(1)).getScreen(anyString());
    verify(reportGenerator, times(1)).generateScreenReportContext(any());
    verify(reportGenerator, times(1)).downloadScreenReportFiles(reportContext.reportFiles());
    verifyNoInteractions(maintainService);
    assertEquals(2, serviceData.getClientActionList().size());
  }

  @Test
  void printScreenMailUsesExplicitReportParameters() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("PdfNam", "/tmp/report.pdf");
    ReportGenerator.GeneratedScreenReportContext reportContext = new ReportGenerator.GeneratedScreenReportContext(Arrays.asList(), parameters);
    when(reportGenerator.generateScreenReportContext(any())).thenReturn(reportContext);
    when(maintainService.launchMaintain("SndRep", parameters)).thenReturn(new ServiceData());

    ServiceData serviceData = reportService.printScreen("screen", "MAIL");

    verify(reportGenerator).generateScreenReportContext(any());
    verify(maintainService).launchMaintain("SndRep", parameters);
    verify(reportGenerator, never()).downloadScreenReportFiles(anyList());
    assertEquals(new ServiceData().getClientActionList(), serviceData.getClientActionList());
  }

  @Test
  void viewPdfFile() throws AWException, IOException {
    // Mock
    final Path file = Files.createFile(Paths.get(tempFolder.getAbsolutePath(), "dummy.pdf"));

    // Test
    ServiceData serviceData = reportService.viewPdfFile(file.toString());

    // Verify
    assertNotNull(serviceData.getData());
  }
}