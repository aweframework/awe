package com.almis.awe.developer.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class PathServiceTest {

  private PathService pathService;
  private Path tempPath;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void setUp() throws Exception {
    tempPath = Files.createTempFile("test", ".properties");
    pathService = new PathService(tempPath.getParent().toString(), tempPath.getFileName().toString(), "test");
    pathService.setApplicationContext(applicationContext);
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.deleteIfExists(tempPath);
  }

  @Test
  void checkPath() throws Exception {
    ServiceData serviceData = pathService.checkPath();
    assertNotNull(serviceData.getDataList());
  }

  @Test
  void getPropertiesFile() throws Exception {
    Properties properties = pathService.getPropertiesFile();
    assertEquals(0, properties.size());
  }

  @Test
  void getPath() throws Exception {
    assertNull(pathService.getPath());
  }

  @Test
  void getPathWithSetPath() throws Exception {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    pathService.setPath("test");
    assertEquals("test", pathService.getPath());
  }

  @Test
  void setPath() throws Exception {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    pathService.setPath("path");
    verify(aweElements, times(2)).getLanguage();
    verify(aweElements, times(2)).getLocaleWithLanguage(anyString(), eq(null));
  }
}