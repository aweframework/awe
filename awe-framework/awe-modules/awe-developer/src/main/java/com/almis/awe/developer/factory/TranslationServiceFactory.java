package com.almis.awe.developer.factory;

import com.almis.awe.developer.translators.ITranslator;
import com.almis.awe.developer.translators.TranslationServiceEnum;
import jakarta.annotation.PostConstruct;

import java.util.List;

public class TranslationServiceFactory {

  private final TranslationServiceEnum translationService;
  private final List<ITranslator> translators;
  private ITranslator selectedTranslator;

  /**
   * Autowired constructor
   *
   * @param translators        Translator list
   * @param translationService Translation service
   */
  public TranslationServiceFactory(List<ITranslator> translators, TranslationServiceEnum translationService) {
    this.translators = translators;
    this.translationService = translationService;
  }

  public ITranslator getTranslator() {
    return selectedTranslator;
  }

  @PostConstruct
  public void selectTranslationService() {
    selectedTranslator = translators.stream().filter(iTranslator -> iTranslator.getClass().getSimpleName().equalsIgnoreCase(translationService.toString())).findFirst().orElse(null);
  }
}
