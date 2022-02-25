package com.almis.awe.service;

import com.almis.awe.service.report.ReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void getPrintActions(boolean show) throws Exception {
    ReflectionTestUtils.setField(reportService, "showPrintOptions", show);
    reportService.getPrintActions();
    verify(queryService, times(1)).launchPrivateQuery(anyString());
  }

  @Test
  void printScreen() throws Exception {
    reportService.printScreen("screen");
    verify(menuService, times(1)).getScreen(anyString());
    verify(reportGenerator, times(1)).generateScreenReport(any());
  }
}