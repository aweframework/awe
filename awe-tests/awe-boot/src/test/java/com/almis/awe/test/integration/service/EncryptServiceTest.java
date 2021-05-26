package com.almis.awe.test.integration.service;

import com.almis.awe.model.util.security.Crypto;
import com.almis.awe.service.EncryptService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class used for testing rest services through ActionController
 *
 * @author pgarcia
 */
@Tag("integration")
@Log4j2
@DisplayName("Encrypt service Tests")
class EncryptServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private EncryptService encryptService;

  /**
   * Check encrypted properties
   */
  @Test
  void checkEncryptedProperty() {
    logger.info("With @Value: " + encryptService.getProperty());
    logger.info("With Environment.getProperty: " + encryptService.getEnvironmentProperty());
    assertEquals("prueba2", encryptService.getProperty());
    assertEquals("prueba2", encryptService.getEnvironmentProperty());
  }

  /**
   * Check pbkdf2 static method
   * @throws Exception exception
   */
  @Test
  void checkPbkdf2() throws Exception {
    String goodValues = Crypto.Utils.pbkdf2("tutu", "dummySalt", 0, 16);
    String badValues = Crypto.Utils.pbkdf2("tutu", "dummySalt", -1, 44);
    assertEquals(goodValues, badValues);
  }

  /**
   * Check random bytes retrieval
   */
  @Test
  void checkGetRandomBytes() {
    byte[] random = Crypto.Utils.getRandomBytes(-1);
    byte[] random2 = Crypto.Utils.getRandomBytes(8);
    assertNotEquals(random, random2);
  }

  /**
   * Check encrypt empty string with AES
   */
  @Test
  void checkEncryptEmptyString() {
    String encrypted = Crypto.AES.encrypt("", "dummyPass", "UTF-8");
    assertNull(encrypted);
  }

  /**
   * Check encrypt with null key with AES
   */
  @Test
  void checkEncryptNullPassphrase() {
    String encrypted = Crypto.AES.encrypt("dummy-test", null, "UTF-8");
    assertNull(encrypted);
  }

  /**
   * Check decrypt empty string with AES
   */
  @Test
  void checkDecryptEmptyString() {
    String decrypted = Crypto.AES.decrypt("", "dummyPass", "UTF-8");
    assertNull(decrypted);
  }

  /**
   * Check decrypt with null key with AES
   */
  @Test
  void checkDecryptNullPassphrase() {
    String decrypted = Crypto.AES.decrypt("encodedText", null, "UTF-8");
    assertNull(decrypted);
  }

  /**
   * Check encrypt with AES a null value
   */
  @Test
  void checkEncryptNull() {
    assertThrows(NullPointerException.class, () -> Crypto.AES.encrypt(null, null, "UTF-8"));
  }

  /**
   * Check decrypt with AES a null value
   */
  @Test
  void checkDecryptNull() {
    assertThrows(NullPointerException.class, () -> Crypto.AES.decrypt(null, null, "UTF-8"));
  }
}