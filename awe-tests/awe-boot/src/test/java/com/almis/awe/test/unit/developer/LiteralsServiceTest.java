package com.almis.awe.test.unit.developer;

import com.almis.awe.developer.service.LiteralsService;
import com.almis.awe.developer.service.LocaleFileService;
import com.almis.awe.developer.service.TranslationService;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Global;
import com.almis.awe.model.entities.locale.Locales;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.test.unit.TestUtil;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Class used for testing translation service
 *
 * @author pvidal
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Log4j2
@RunWith(MockitoJUnitRunner.class)
public class LiteralsServiceTest extends TestUtil {

  @InjectMocks
  private LiteralsService literalsService;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  private TranslationService translationService;

  @Mock
  private LocaleFileService localeFileService;

  /**
   * Initializes beans for tests
   */
  @Before
  public void initBeans() throws Exception {
    doReturn(aweElements).when(context).getBean(AweElements.class);
    ReflectionTestUtils.setField(literalsService, "defaultLanguage", "en");
  }

  /**
   * Test translate a text with the same language from/to
   * Skip call api request
   *
   * @throws AWException AWE exception
   */
  @Test
  public void translateSameLanOriginDestination() throws AWException {
    // Launch
    ServiceData serviceData = literalsService.translate("This is a test", "en", "en");

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
  public void translate() throws AWException {
    // Mockito actions
    when(translationService.getTranslation(anyString(), anyString(), anyString())).thenReturn("Esto es una prueba");

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
  public void switchLanguages() {
    ServiceData serviceData = literalsService.switchLanguages("EN", "ES", "en", "es");
    assertEquals(2, serviceData.getClientActionList().size());
  }

  /**
   * Test switch Languages
   */
  @Test
  public void newLiteral() throws Exception {
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("OK");
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es", "en", "fr"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.newLiteral("en", "TEST", "Test");
    assertEquals("OK", serviceData.getTitle());
  }

  /**
   * Test switch Languages
   */
  @Test
  public void newLiteralWithError() throws Exception {
    when(translationService.getTranslation(anyString(), anyString(), anyString())).thenThrow(new AWException("ERROR", "VAYA"));
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es", "en", "fr"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.newLiteral("en", "TEST", "Test");
    assertEquals(AnswerType.ERROR, serviceData.getType());
    assertEquals("ERROR", serviceData.getTitle());
    assertEquals("VAYA", serviceData.getMessage());
  }

  /**
   * Test store updated locale
   */
  @Test
  public void saveTranslation() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.saveTranslation("TEXT", "texto", null, "es", "en", "TEXTO");
    assertEquals(AnswerType.OK, serviceData.getType());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  public void saveTranslationWithMarkdown() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(new ArrayList<>()));
    ServiceData serviceData = literalsService.saveTranslation("MARKDOWN", null, "texto", "es", "es", "TEXTO");
    assertEquals(AnswerType.OK, serviceData.getType());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test(expected = AWException.class)
  public void saveTranslationErrorStoring() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenThrow(new AWException("ea"));
    ServiceData serviceData = literalsService.saveTranslation("MARKDOWN", null, "texto", "es", "es", "TEXTO");
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  public void getSelectedLocale() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
      new Global().setName("TATA").setValue("lala"),
      new Global().setName("TEXT").setValue("eeeo"),
      new Global().setName("LOLO").setValue("lalo")
    )));
    ServiceData serviceData = literalsService.getSelectedLocale("es", "TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(4, serviceData.getClientActionList().size());
  }

  /**
   * Test store updated locale with markdown
   */
  @Test
  public void getSelectedLocaleWithMarkdown() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
      new Global().setName("TATA").setMarkdown("lala"),
      new Global().setName("TEXT").setMarkdown("eeeo"),
      new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getSelectedLocale("es", "TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(4, serviceData.getClientActionList().size());
  }

  /**
   * Test delete literal
   */
  @Test
  public void deleteLiteral() throws Exception {
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("OK");
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es", "en", "fr"));
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
  public void getLocaleMatches() throws Exception {
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
      new Global().setName("TATA").setMarkdown("lala"),
      new Global().setName("TEXT").setMarkdown("eeeo"),
      new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getLocaleMatches("la", "en");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(2, serviceData.getDataList().getRows().size());
  }

  /**
   * Test get locale matches
   */
  @Test
  public void getTranslationList() throws Exception {
    when(localeFileService.getLanguageList()).thenReturn(Arrays.asList("es", "en", "fr"));
    when(localeFileService.readLocalesFromFile(anyString())).thenReturn(new Locales().setLocales(Arrays.asList(
      new Global().setName("TATA").setMarkdown("lala"),
      new Global().setName("TEXT").setMarkdown("eeeo"),
      new Global().setName("LOLO").setMarkdown("lalo")
    )));
    ServiceData serviceData = literalsService.getTranslationList("TEXT");
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(3, serviceData.getDataList().getRows().size());
    logger.info(serviceData.getDataList());
  }

  /**
   * Test get locale matches
   */
  @Test
  public void getUsingLanguage() throws Exception {
    ServiceData serviceData = literalsService.getUsingLanguage();
    assertEquals(AnswerType.OK, serviceData.getType());
    assertEquals(1, serviceData.getDataList().getRows().size());
    logger.info(serviceData.getDataList());
  }
}