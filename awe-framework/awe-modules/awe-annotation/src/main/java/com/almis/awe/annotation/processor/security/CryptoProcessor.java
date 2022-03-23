package com.almis.awe.annotation.processor.security;

import com.almis.awe.annotation.aspect.CryptoAnnotation;
import com.almis.awe.annotation.entities.security.Crypto;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.EncodeService;
import lombok.extern.slf4j.Slf4j;

/**
 * Processor for the Crypto annotation
 *
 * @see CryptoAnnotation
 * @see Crypto
 * @author dfuentes
 * Created by dfuentes on 18/04/2017.
 */
@Slf4j
public class CryptoProcessor {

  private final EncodeService encodeService;

  // Constructor
  public CryptoProcessor(EncodeService encodeService) {
    this.encodeService = encodeService;
  }

  /**
   * Process current annotation
   *
   * @param crypto Crypto text
   * @param text Value
   * @return Processed value
   */
  public String processCrypto(Crypto crypto, String text) {
    String processedValue = null;
    try {
      switch (crypto.action()) {
        case ENCRYPT:
          processedValue = encodeService.encryptAes(text, crypto.password());
          break;
        case DECRYPT:
          processedValue = encodeService.decryptAes(text, crypto.password());
          break;
        default:
          processedValue = text;
      }
    } catch (AWException exc) {
      log.error(exc.getMessage(), exc);
    }
    return processedValue;
  }
}
