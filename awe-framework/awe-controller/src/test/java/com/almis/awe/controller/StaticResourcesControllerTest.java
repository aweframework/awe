package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.Theme;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticResourcesControllerTest {

  @InjectMocks
  private StaticResourcesController staticResourcesController;

  @Mock
  private QueryService queryService;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Test
  void getThemeVariables() throws Exception {
    // Mock
    DataList dataList = DataListUtil.fromBeanList(List.of(new Theme().setName("test"), new Theme().setName("test").setDark(true)));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));
    when(baseConfigProperties.isThemeFollowOsColorScheme()).thenReturn(true);

    // Do
    String result = staticResourcesController.getThemeVariables("xxx");

    // Verify
    assertEquals("""
      .theme-test {
      }

      .theme-test.dark {
      }

      @media (prefers-color-scheme: dark) {
        .theme-test:not(.light):not(.dark) {
        }
      }
      """, result);
  }

  @Test
  void getThemeVariablesWithoutFollowingTheOperatingSystem() throws Exception {
    // Mock
    DataList dataList = DataListUtil.fromBeanList(List.of(new Theme().setName("test").setDark(true)));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));
    when(baseConfigProperties.isThemeFollowOsColorScheme()).thenReturn(false);

    // Do
    String result = staticResourcesController.getThemeVariables("xxx");

    // Verify
    assertEquals("""
      .theme-test.dark {
      }
      """, result);
  }
}
