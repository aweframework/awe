package com.almis.awe.developer.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.locale.Locales;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Class used for testing translation service
 *
 * @author pvidal
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class LocaleFileServiceTest {

  @InjectMocks
  private LocaleFileService localeFileService;
  @Mock
  private PathService pathService;
  @Mock
  private BaseConfigProperties baseConfigProperties;
  @Mock
  private XStreamSerializer serializer;
  @TempDir
  static File tempFolder;

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  void readLocalesFromFile() throws Exception {
    when(pathService.getPath()).thenReturn(tempFolder.getAbsolutePath() + "/");
    when(serializer.getObjectFromXml(eq(Locales.class), any(InputStream.class))).thenReturn(new Locales());
    when(baseConfigProperties.getFiles()).thenReturn(new BaseConfigProperties.Files());
    when(baseConfigProperties.getExtensionXml()).thenReturn(".xml");
    Files.createFile(Paths.get(tempFolder.getPath(), "Locale-EN.xml"));
    // Launch
    Locales locales = localeFileService.readLocalesFromFile("en");
    assertNotNull(locales);
  }

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  void readLocalesFromFileNotFound() throws Exception {
    // Launch
    when(baseConfigProperties.getFiles()).thenReturn(new BaseConfigProperties.Files());
    when(baseConfigProperties.getExtensionXml()).thenReturn(".xml");
    Assertions.assertNull(localeFileService.readLocalesFromFile("en"));
  }
}