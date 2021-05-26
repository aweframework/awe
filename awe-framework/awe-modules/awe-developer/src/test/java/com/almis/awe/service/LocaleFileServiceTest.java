package com.almis.awe.service;

import com.almis.awe.developer.service.LocaleFileService;
import com.almis.awe.developer.service.PathService;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.locale.Locales;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
@Log4j2
@ExtendWith(MockitoExtension.class)
class LocaleFileServiceTest {

  @InjectMocks
  private LocaleFileService localeFileService;
  @Mock
  private PathService pathService;
  @Mock
  private XStreamSerializer serializer;
  @TempDir
  static File tempFolder;

  /**
   * Initializes beans for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    ReflectionTestUtils.setField(localeFileService, "xmlExtension", ".xml");
    ReflectionTestUtils.setField(localeFileService, "localeFile", "Locale-");
  }

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
    Assertions.assertNull(localeFileService.readLocalesFromFile("en"));
  }
}