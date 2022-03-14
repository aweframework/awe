package com.almis.awe.developer.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.locale.Locales;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.constraints.Size;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

  @BeforeAll
  static void setUp() {
    final List<@Size(min = 2, max = 2) String> languageCodeList = Arrays.asList("EN", "ES", "FR");
    // Given
    createLocaleFiles(tempFolder, languageCodeList);
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
    when(baseConfigProperties.getFiles()).thenReturn(new BaseConfigProperties.Files());
    when(baseConfigProperties.getExtensionXml()).thenReturn(".xml");
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

  /**
   * Test get language list
   * @throws AWException AWE exception
   */
  @Test
  void getLanguageListTest() throws AWException {
    // Given
    when(pathService.getPath()).thenReturn(tempFolder.getAbsolutePath() + "/");
    when(baseConfigProperties.getFiles()).thenReturn(new BaseConfigProperties.Files());
    when(baseConfigProperties.getExtensionXml()).thenReturn(".xml");
    // Do
    final List<String> languageList = localeFileService.getLanguageList();
    // Asserts
    assertNotNull(languageList, "Error, the list not should be null");
    assertEquals(3, languageList.size());
  }

  /**
   * Test store locale list file
   * @throws AWException AWE exception
   */
  @Test
  void givenDummyLocal_storeLocaleListFileTest_notThrowException() throws AWException {
    // Given
    final Global dummyLocal = new Global()
            .setName("dummy")
            .setLabel("DUMMY_LABEL")
            .setValue("dummy");
    when(baseConfigProperties.getFiles()).thenReturn(new BaseConfigProperties.Files());
    when(baseConfigProperties.getExtensionXml()).thenReturn(".xml");
    when(pathService.getPath()).thenReturn(tempFolder.getAbsolutePath());
    // Do
    assertDoesNotThrow(() ->
            localeFileService.storeLocaleListFile("en", new Locales().setLocales(Collections.singletonList(dummyLocal))));
  }

  private static void createLocaleFiles(File tempFolder, List<String> languageList) {
    languageList.forEach(lang -> {
      try {
        Files.createFile(Paths.get(tempFolder.getAbsolutePath(), "Locale-" + lang + ".xml"));
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    });
  }
}