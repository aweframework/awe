package com.almis.awe.developer.autoconfigure;

import com.almis.awe.developer.factory.TranslationServiceFactory;
import com.almis.awe.developer.service.LiteralsService;
import com.almis.awe.developer.service.LocaleFileService;
import com.almis.awe.developer.service.PathService;
import com.almis.awe.developer.service.TranslationService;
import com.almis.awe.developer.translators.ITranslator;
import com.almis.awe.developer.util.LocaleUtil;
import com.almis.awe.model.component.XStreamSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@EnableConfigurationProperties({DeveloperConfigProperties.class})
public class DeveloperConfig {

  private final Environment environment;
  private final DeveloperConfigProperties developerConfigProperties;

  @Autowired
  public DeveloperConfig(Environment environment, DeveloperConfigProperties developerConfigProperties) {
    this.environment = environment;
    this.developerConfigProperties = developerConfigProperties;
  }

  /**
   * Path management service
   *
   * @return Path management bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PathService pathService() {
    return new PathService(developerConfigProperties.getPath(), developerConfigProperties.getPathFile(), developerConfigProperties.getPathProperty());
  }

  /**
   * Rest template
   *
   * @return Rest template
   */
  @Bean
  @ConditionalOnMissingBean
  public RestTemplate localeRestTemplate() {
    return new RestTemplate();
  }

  /**
   * Translator factory
   *
   * @param translatorList Translator list
   * @return Translator service
   */
  @Bean
  @ConditionalOnMissingBean
  public TranslationServiceFactory translationServiceFactory(List<ITranslator> translatorList) {
    return new TranslationServiceFactory(translatorList, developerConfigProperties.getTranslationService());
  }

  /**
   * Translation service
   *
   * @return Translation service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public TranslationService translationService(TranslationServiceFactory translationServiceFactory) {
    return new TranslationService(translationServiceFactory);
  }

  /**
   * Locale file service
   *
   * @return Locale file bean
   */
  @Bean
  @ConditionalOnMissingBean
  public LocaleFileService localeFileService(PathService pathService, XStreamSerializer serializer) {
    return new LocaleFileService(pathService, serializer);
  }

  /**
   * Literals management service
   *
   * @return Literals management bean
   */
  @Bean
  @ConditionalOnMissingBean
  public LiteralsService literalsService(TranslationService translationService, LocaleFileService localeFileService) {
    return new LiteralsService(translationService, localeFileService);
  }

  /**
   * Initialize static utilities
   */
  @PostConstruct
  public void initializeStaticUtilities() {
    LocaleUtil.init(environment);
  }

}
