package com.almis.awe.developer.translators.clients;

import com.almis.awe.developer.model.TranslationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@FeignClient(name = "MyMemoryClient", url = "https://${translation.mymemory.host:}")
public interface MyMemoryFeignClient {

  @GetMapping("/api/get?langpair={languageFrom}|{languageTo}")
  TranslationResponse translate(URI baseUrl, @RequestParam("q") String text,
                                               @PathVariable("languageFrom") String languageFrom, @PathVariable("languageTo") String languageTo,
                                               @RequestParam("key") String key, @RequestParam("de") String email);
}
