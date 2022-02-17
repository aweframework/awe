package com.almis.awe.developer.translators.clients;

import com.almis.awe.developer.model.TranslationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;

@FeignClient(name = "RapidAPIClient", url = "https://${translation.rapidapi.host:}")
public interface RapidAPIFeignClient {

  @GetMapping("/api/get?langpair={languageFrom}|{languageTo}")
  ResponseEntity<TranslationResponse> translate(URI baseUrl, @RequestParam("q") String text,
                                               @PathVariable("languageFrom") String languageFrom, @PathVariable("languageTo") String languageTo,
                                               @RequestParam("key") String myMemoryKey, @RequestParam("de") String email,
                                               @RequestHeader("X-RapidAPI-Host") String host, @RequestHeader("X-RapidAPI-Key") String key);
}
