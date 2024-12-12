package com.almis.awe.developer.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.developer.model.Translation;
import com.almis.awe.developer.model.TranslationResponse;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Class used for testing translation service
 *
 * @author pvidal
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class LiteralsServiceTest {

  @InjectMocks
  private LiteralsService literalsService;

  @Mock
  private ApplicationContext context;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private AweElements aweElements;

  @Mock
  private TranslationService translationService;

  @Mock
  private LocaleFileService localeFileService;

  /**
   * Initializes beans for tests
   */
  @BeforeEach
  void initBeans() {
    literalsService.setApplicationContext(context);
  }

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  void translateSameLanOriginDestination() throws AWException {
    // Launch
    ServiceData serviceData = literalsService.translate("This is a test", "en-GB", "en-GB");

    // Asserts and verifications
    assertNotNull(serviceData.getDataList());
    assertEquals("This is a test", DataListUtil.getData(serviceData.getDataList(), 0, "value"));
  }

  /**
   * Test translate a text
   *
   * @throws AWException AWE exception
   */
  @Test
  void translate() throws AWException {
    // Mockito actions
    when(translationService.getTranslation(anyString(), anyString(), anyString())).thenReturn(new TranslationResponse().setResponseData(new Translation().setTranslatedText("Esto es una prueba")));

    // Launch
    ServiceData serviceData = literalsService.translate("This is a test", "en", "es");

    // Asserts and verifications
    assertNotNull(serviceData.getDataList());
    assertEquals("Esto es una prueba", DataListUtil.getData(serviceData.getDataList(), 0, "value"));
  }

  /**
   * Test switch Languages
   */
  @Test
  void switchLanguages() {
    ServiceData serviceData = literalsService.switchLanguages("en-GB", "es-ES", "en-GB", "es-ES");
    assertEquals(2, serviceData.getClientActionList().size());
  }

  /**
   * Test switch Languages
   */
  @Test
  void newLiteral() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("OK");
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es-ES", "en-GB", "fr-FR"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    when(translationService.getTranslation(anyString(), anyString(), anyString())).thenReturn(new TranslationResponse().setResponseData(new Translation()));
    ServiceData serviceData = literalsService.newLiteral("en-GB", "TEST", "Test");
    assertEquals("OK", serviceData.getTitle());
  }

  /**
   * Test switch Languages
   */
  @Test
  void newLiteralWithRemaining() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("OK");
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es-ES", "en-GB", "fr-FR"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    when(translationService.getTranslation(anyString(), anyString(), eq("es-ES"))).thenReturn(new TranslationResponse().setRemaining("9978").setResponseData(new Translation()));
    when(translationService.getTranslation(anyString(), anyString(), eq("fr-FR"))).thenReturn(new TranslationResponse().setRemaining("9975").setResponseData(new Translation()));
    ServiceData serviceData = literalsService.newLiteral("en-GB", "TEST", "Test");
    assertEquals("OK", serviceData.getTitle());
  }

  /**
   * Test switch Languages
   */
  @Test
  void newLiteralWithError() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(translationService.getTranslation(anyString(), anyString(), anyString())).thenThrow(new AWException("ERROR", "VAYA"));
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es-ES", "en-GB", "fr-FR"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.newLiteral("en-GB", "TEST", "Test");
    assertEquals(AnswerType.ERROR, serviceData.getType());
    assertEquals("ERROR", serviceData.getTitle());
    assertEquals("VAYA", serviceData.getMessage());
  }

  /**
   * Test store updated locale
   */
  @Test
  void saveTranslation() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.saveTranslation("TEXT", "texto", null, "es-ES", "en-GB", "TEXTO");
    assertEquals(AnswerType.OK, serviceData.getType());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  void saveTranslationWithMarkdown() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.saveTranslation("MARKDOWN", null, "texto", "es-ES", "es-ES", "TEXTO");
    assertEquals(AnswerType.OK, serviceData.getType());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  void saveTranslationErrorStoring() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(localeFileService.readLocalesFromFile(anyString())).thenThrow(new AWException("ea"));
    assertThrows(AWException.class, () -> literalsService.saveTranslation("MARKDOWN", null, "texto", "es-ES", "es-ES", "TEXTO"));
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  void getSelectedLocale() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
            new Global().setName("TATA").setValue("lala"),
            new Global().setName("TEXT").setValue("eeeo"),
            new Global().setName("LOLO").setValue("lalo")
    )));
    ServiceData serviceData = literalsService.getSelectedLocale("es-ES", "TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(4, serviceData.getClientActionList().size());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  void getSelectedLocaleWithMarkdown() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
            new Global().setName("TATA").setMarkdown("lala"),
            new Global().setName("TEXT").setMarkdown("eeeo"),
            new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getSelectedLocale("es-ES", "TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(4, serviceData.getClientActionList().size());
  }

  /**
   * Test delete literal
   */
  @Test
  void deleteLiteral() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("OK");
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es-ES", "en-GB", "fr-FR"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
            new Global().setName("TATA").setMarkdown("lala"),
            new Global().setName("TEXT").setMarkdown("eeeo"),
            new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.deleteLiteral("TEXT");
    assertEquals("OK", serviceData.getTitle());
  }

  /**
   * Test get locale matches
   */
  @Test
  void getLocaleMatches() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
            new Global().setName("TATA").setMarkdown("lala"),
            new Global().setName("TEXT").setMarkdown("eeeo"),
            new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getLocaleMatches("la", "en-GB");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(2, serviceData.getDataList().getRows().size());
  }

  /**
   * Test get locale matches
   */
  @Test
  void getTranslationList() throws Exception {
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es-ES", "en-GB", "fr-FR"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
            new Global().setName("TATA").setMarkdown("lala"),
            new Global().setName("TEXT").setMarkdown("eeeo"),
            new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getTranslationList("TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(3, serviceData.getDataList().getRows().size());
    log.info(serviceData.getDataList().toString());
  }

  /**
   * Test get locale matches
   */
  @Test
  void getUsingLanguage() {
    when(baseConfigProperties.getLanguageDefault()).thenReturn("en-GB");
    ServiceData serviceData = literalsService.getUsingLanguage();
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(1, serviceData.getDataList().getRows().size());
    log.info(serviceData.getDataList().toString());
  }
}