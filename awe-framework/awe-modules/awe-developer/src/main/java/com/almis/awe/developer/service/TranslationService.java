package com.almis.awe.developer.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.developer.model.TranslationResponse;
import com.almis.awe.exception.AWException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
public class TranslationService extends ServiceConfig {

  // Services
  private final RestTemplate restTemplate;

  @Value("${translation.api.key}")
  private String translationApiKey;
  @Value("${translation.api.url}")
  private String translationApiUrl;
  @Value("${translation.api.parameters.key}")
  private String keyParameter;
  @Value("${translation.api.parameters.language}")
  private String languageParameter;
  @Value("${translation.api.parameters.text}")
  private String textParameter;

  /**
   * Autowired constructor
   *
   * @param restTemplate Rest template
   */
  @Autowired
  public TranslationService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
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
  public String getTranslation(String literal, String fromLang, String toLang) throws AWException {
    try {
      // Get translation from cloud
      ResponseEntity<TranslationResponse> responseResponseEntity = callTranslationApi(literal, fromLang, toLang);

      if (HttpStatus.OK.equals(responseResponseEntity.getStatusCode())) {
        return Objects.requireNonNull(responseResponseEntity.getBody()).getResponseData().getTranslatedText();
      } else {
        throw new AWException(getLocale("ERROR_TITLE_RETRIEVING_TRANSLATION"),
          getLocale("ERROR_MESSAGE_RETRIEVING_TRANSLATION", toLang, literal));
      }
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_RETRIEVING_TRANSLATION"),
        getLocale("ERROR_MESSAGE_RETRIEVING_TRANSLATION", toLang, literal), exc);
    }
  }

  /**
   * Returns the URL to make a call for translation to the API
   *
   * @param literal  Locale
   * @param fromLang Source language
   * @param toLang   Target language
   * @return Url string
   */
  private ResponseEntity<TranslationResponse> callTranslationApi(String literal, String fromLang, String toLang) {
    return restTemplate.getForEntity(
      String.format("%s?%s={apiKey}&%s={fromLanguage}|{toLanguage}&%s={text}", translationApiUrl, keyParameter, languageParameter, textParameter),
      TranslationResponse.class, translationApiKey, fromLang.toLowerCase(), toLang.toLowerCase(), literal);
  }
}
