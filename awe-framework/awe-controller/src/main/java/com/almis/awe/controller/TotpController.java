package com.almis.awe.controller;

import com.almis.awe.service.TotpService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/access")
public class TotpController {

  // Services
  private final TotpService totpService;

  /**
   * Autowired controller
   * @param totpService TOTP service
   */
  public TotpController(TotpService totpService) {
    this.totpService = totpService;
  }

  /**
   * Handler for index page
   *
   * @return Index page
   */
  @GetMapping(value = {"/qr-code"}, produces = "image/png")
  public ResponseEntity<byte[]> getQRCode() throws QrGenerationException {
    return ResponseEntity.ok()
      .cacheControl(CacheControl.maxAge(1, TimeUnit.SECONDS))
      .body(totpService.getQRCode());
  }
}
