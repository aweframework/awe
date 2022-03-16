package com.almis.awe.tools.service;

import com.almis.awe.config.BaseConfigProperties;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileManagerServiceTest {

  @Mock
  BaseConfigProperties.Filemanager filemanager;
  @InjectMocks
  private FileManagerService fileManagerService;
  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Test
  void afterPropertiesSet() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(Files.temporaryFolderPath());
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(2)).getDateFormat();
  }

  @Test
  void afterPropertiesSetBadDirectory() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn("");
    assertThrows(IllegalArgumentException.class, () -> fileManagerService.afterPropertiesSet());
    verify(filemanager, times(0)).getDateFormat();
  }

  @Test
  void afterPropertiesSetBadDateFormat() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(Files.temporaryFolderPath());
    when(filemanager.getDateFormat()).thenReturn("");
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(3)).getDateFormat();
  }

  @Test
  void afterPropertiesSetGoodDateFormat() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(Files.temporaryFolderPath());
    when(filemanager.getDateFormat()).thenReturn("dd/MM/yyyy");
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(2)).getDateFormat();
  }
}