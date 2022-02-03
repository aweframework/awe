package com.almis.awe.developer.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.developer.factory.TranslationServiceFactory;
import com.almis.awe.developer.model.ITranslationResult;
import com.almis.awe.exception.AWException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TranslationService extends ServiceConfig {

  private final TranslationServiceFactory translationServiceFactory;

  /**
   * Autowired constructor
   *
   * @param translationServiceFactory Translation service factory
   */
  public TranslationService(TranslationServiceFactory translationServiceFactory) {
    this.translationServiceFactory = translationServiceFactory;
  }

  /**
   * Extract translation from API result
   *
   * @param literal  Locale
   * @param fromLang Source language
   * @param toLang   Target language
   * @return Locale translated
   * @throws AWException Error translating locale
   */
  public ITranslationResult getTranslation(String literal, String fromLang, String toLang) throws AWException {
    try {
      return translationServiceFactory.getTranslator().translate(literal, fromLang, toLang);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_RETRIEVING_TRANSLATION"),
        getLocale("ERROR_MESSAGE_RETRIEVING_TRANSLATION", toLang, literal), exc);
    }
  }
}
