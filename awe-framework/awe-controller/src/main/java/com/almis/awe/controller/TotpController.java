package com.almis.awe.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.service.TotpService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
   * Serve the authenticated user's TOTP QR code.
   * <p>
   * Only accessible in allowed flows (fully authenticated settings activation,
   * or just-completed FORCE enrollment before TOTP verification).
   * Partially authenticated sessions with enrolled 2FA are denied.
   *
   * @return QR code PNG response
   * @throws AWException when the session is not permitted to access this endpoint
   */
  @GetMapping(value = {"/qr-code"}, produces = "image/png")
  public ResponseEntity<byte[]> getQRCode() throws AWException {
    return ResponseEntity.ok()
      .contentType(MediaType.IMAGE_PNG)
      .cacheControl(CacheControl.noStore().cachePrivate().mustRevalidate())
      .header(HttpHeaders.PRAGMA, "no-cache")
      .header(HttpHeaders.EXPIRES, "0")
      .body(totpService.getQRCode());
  }
}
