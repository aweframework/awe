package com.almis.awe.controller;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.Theme;
import com.almis.awe.model.service.DataListService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticResourcesControllerTest {

  @InjectMocks
  private StaticResourcesController staticResourcesController;

  @Mock
  private QueryService queryService;

  @Mock
  private DataListService dataListService;

  @Test
  void getThemeVariables() throws Exception {
    // Mock
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(new DataList()));
    when(dataListService.asBeanList(any(), any())).thenReturn(Arrays.asList(new Theme().setName("test"), new Theme().setName("test").setDark(true)));

    // Do
    String result = staticResourcesController.getThemeVariables();

    // Verify
    assertEquals("""
      .theme-test {
      }

      .theme-test.dark {
      }
      """, result);
  }
}