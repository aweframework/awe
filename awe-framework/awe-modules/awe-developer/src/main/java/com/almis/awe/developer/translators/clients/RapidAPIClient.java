package com.almis.awe.developer.translators.clients;

import com.almis.awe.developer.model.TranslationResponse;
import com.almis.awe.developer.translators.ITranslator;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class RapidAPIClient implements ITranslator {

  private final RapidAPIFeignClient rapidAPIFeignClient;
  private final String key;
  private final String host;
  private final String email;
  private final String myMemoryKey;

  public RapidAPIClient(RapidAPIFeignClient rapidAPIFeignClient, String key, String host, String email, String myMemoryKey) {
    this.rapidAPIFeignClient = rapidAPIFeignClient;
    this.key = key;
    this.myMemoryKey = myMemoryKey;
    this.host = host;
    this.email = email;
  }

  public TranslationResponse translate(String text, String languageFrom, String languageTo) throws URISyntaxException {
    ResponseEntity<TranslationResponse> responseEntity = rapidAPIFeignClient.translate(new URI("https://" + host), text, languageFrom, languageTo, myMemoryKey, email, host, key);
    TranslationResponse response = Optional.ofNullable(responseEntity.getBody()).orElse(new TranslationResponse());
    response.setRemaining(responseEntity.getHeaders().getFirst("x-ratelimit-words-remaining"));
    return response;
  }
}
