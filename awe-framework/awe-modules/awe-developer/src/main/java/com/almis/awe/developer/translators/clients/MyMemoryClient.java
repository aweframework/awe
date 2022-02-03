package com.almis.awe.developer.translators.clients;

import com.almis.awe.developer.model.TranslationResponse;
import com.almis.awe.developer.translators.ITranslator;

import java.net.URI;
import java.net.URISyntaxException;

public class MyMemoryClient implements ITranslator {

  private final MyMemoryFeignClient myMemoryFeignClient;
  private final String key;
  private final String host;
  private final String email;

  public MyMemoryClient(MyMemoryFeignClient myMemoryFeignClient, String key, String host, String email) {
    this.myMemoryFeignClient = myMemoryFeignClient;
    this.key = key;
    this.host = host;
    this.email = email;
  }

  public TranslationResponse translate(String text, String languageFrom, String languageTo) throws URISyntaxException {
    return myMemoryFeignClient.translate(new URI("https://" + host), text, languageFrom, languageTo, key, email);
  }
}
