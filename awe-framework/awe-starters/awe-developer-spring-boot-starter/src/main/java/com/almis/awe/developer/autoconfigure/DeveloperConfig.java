package com.almis.awe.developer.autoconfigure;

import com.almis.awe.developer.service.LiteralsService;
import com.almis.awe.developer.service.LocaleFileService;
import com.almis.awe.developer.service.PathService;
import com.almis.awe.developer.service.TranslationService;
import com.almis.awe.developer.util.LocaleUtil;
import com.almis.awe.model.component.XStreamSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource(value = "classpath:config/developer.properties")
public class DeveloperConfig {

  // Autowired services
  Environment environment;

  /**
   * Autowired constructor
   *
   * @param environment Environment
   */
  @Autowired
  public DeveloperConfig(Environment environment) {
    this.environment = environment;
  }

  /**
   * Path management service
   *
   * @return Path management bean
   */
  @Bean
  @ConditionalOnMissingBean
  public PathService pathService() {
    return new PathService();
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
   * Translation service
   *
   * @return Translation service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public TranslationService translationService() {
    return new TranslationService(localeRestTemplate());
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
