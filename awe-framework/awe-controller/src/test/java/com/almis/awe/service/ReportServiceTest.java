package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.report.ReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @InjectMocks
  ReportService reportService;

  @Mock
  ReportGenerator reportGenerator;

  @Mock
  MenuService menuService;

  @Mock
  QueryService queryService;

  @Mock
  BaseConfigProperties baseConfigProperties;

  @TempDir
  static File tempFolder;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void getPrintActions(boolean show) throws Exception {
    when(baseConfigProperties.isPrintAllOptionsEnable()).thenReturn(true);
    reportService.getPrintActions();
    verify(queryService, times(1)).launchPrivateQuery(anyString());
  }

  @Test
  void printScreen() throws Exception {
    reportService.printScreen("screen");
    verify(menuService, times(1)).getScreen(anyString());
    verify(reportGenerator, times(1)).generateScreenReport(any());
  }

  @Test
  void viewPdfFile() throws AWException, IOException {
    // Given
    final Path file = Files.createFile(Paths.get(tempFolder.getAbsolutePath(), "dummy.pdf"));
    // When
    ServiceData serviceData = reportService.viewPdfFile(file.toString());
    // Then
    assertNotNull(serviceData.getData());
  }
}