package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.util.security.Crypto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncodeServiceTest {

  @InjectMocks
  private EncodeService encodeService;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private SecurityConfigProperties securityConfigProperties;

  @Test
  void encodeRipEmd160() {
    assertEquals("72c931bcdede01f4b5ef55a9a4f40405e3d516cb", EncodeService.encodeRipEmd160("prueba"));
  }

  @Test
  void decryptRipEmd160() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    when(securityConfigProperties.getMasterKey()).thenReturn("test");
    assertEquals("prueba", encodeService.decryptRipEmd160("3rkH/bCfyjE="));
  }

  @Test
  void encryptRipEmd160() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    when(securityConfigProperties.getMasterKey()).thenReturn("test");
    assertEquals("3rkH/bCfyjE=", encodeService.encryptRipEmd160("prueba"));
  }

  @Test
  void encryptRipEmd160WithPhraseKey() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("3rkH/bCfyjE=", encodeService.encryptRipEmd160WithPhraseKey("prueba", "test"));
  }

  @Test
  void testEncryptRipEmd160WithPhraseKey() throws Exception {
    assertEquals("3rkH/bCfyjE=", encodeService.encryptRipEmd160WithPhraseKey("prueba", "test", "UTF-8"));
  }

  @Test
  void decryptRipEmd160WithPhraseKey() throws Exception {
    assertEquals("prueba", encodeService.decryptRipEmd160WithPhraseKey("3rkH/bCfyjE=", "test", "UTF-8"));
  }

  @Test
  void encryptAes() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    when(securityConfigProperties.getMasterKey()).thenReturn("test");
    String encrypted = encodeService.encryptAes("prueba");
    assertNotEquals("prueba", encrypted);
    assertEquals("prueba", encodeService.decryptAes(encrypted, "test"));
  }

  @Test
  void testEncryptAes() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    String encrypted = encodeService.encryptAes("prueba", "test");
    assertNotEquals("prueba", encrypted);
    assertEquals("prueba", encodeService.decryptAes(encrypted, "test"));
  }

  @Test
  void encodeSymmetric() throws Exception {
    assertEquals("dGVzdA", EncodeService.encodeSymmetric("test"));
  }

  @Test
  void testEncodeSymmetric() throws Exception {
    assertEquals("dGVzdA", EncodeService.encodeSymmetric("test".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void decodeSymmetric() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("test", encodeService.decodeSymmetric("dGVzdA"));
  }

  @Test
  void decodeSymmetricAsByteArray() throws Exception {
    assertEquals("test", new String(encodeService.decodeSymmetricAsByteArray("dGVzdA"), StandardCharsets.UTF_8));
  }

  @Test
  void encodeHex() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("74657374", encodeService.encodeHex("test"));
  }

  @Test
  void decodeHex() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("test", encodeService.decodeHex("74657374"));
  }

  @Test
  void encodeTransmission() throws Exception {
    assertEquals("dGVzdA", encodeService.encodeTransmission("test", true));
    assertEquals("test", encodeService.encodeTransmission("test", false));
  }

  @Test
  void decodeTransmission() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("test", encodeService.decodeTransmission("dGVzdA", true));
    assertEquals("test", encodeService.decodeTransmission("test", false));
  }

  @Test
  void hash() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("00000000000000000000000000000000098f6bcd4621d373cade4e832627b4f6", encodeService.hash(EncodeService.HashingAlgorithms.MD5, "test"));
  }

  @Test
  void testHash() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("00000000000000000000000000000000098f6bcd4621d373cade4e832627b4f6", encodeService.hash(EncodeService.HashingAlgorithms.MD5, "test", "1212"));
  }

  @Test
  void encodePBKDF2WithHmacSHA1() throws Exception {
    when(securityConfigProperties.getMasterKey()).thenReturn("test");
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals(64, encodeService.encodePBKDF2WithHmacSHA1("test").length());
  }

  @Test
  void testEncodePBKDF2WithHmacSHA1() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals(64, encodeService.encodePBKDF2WithHmacSHA1("test", "test").length());
  }

  @Test
  void testEncodePBKDF2WithHmacSHA11() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals(64, encodeService.encodePBKDF2WithHmacSHA1("test", "test", Crypto.Utils.getRecommendedIterationNumber()).length());
  }

  @Test
  void testEncodePBKDF2WithHmacSHA12() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals(64, encodeService.encodePBKDF2WithHmacSHA1("test", "test", Crypto.Utils.getRecommendedIterationNumber(), 256).length());
  }

  @Test
  void getSecureRandom() {
    assertNotNull(EncodeService.getSecureRandom());
  }

  @Test
  void getSecureRandomString() {
    assertNotNull(encodeService.getSecureRandomString());
  }

  @Test
  void encodeLanguage() throws Exception {
    assertEquals("test", EncodeService.encodeLanguage("test", "UTF-8"));
  }

  @Test
  void base64Encode() {
    assertEquals("dGVzdA", EncodeService.base64Encode("test".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testBase64Encode() {
    assertEquals("dGVzdA", EncodeService.base64Encode("test".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void base64Decode() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("test", encodeService.base64Decode("dGVzdA"));
  }


  @Test
  void testBase64Decode() throws Exception {
    when(baseConfigProperties.getEncoding()).thenReturn("UTF-8");
    assertEquals("test", encodeService.base64Decode("dGVzdA".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void base64DecodeAsByteArray() {
    assertEquals("test", new String(encodeService.base64DecodeAsByteArray("dGVzdA"), StandardCharsets.UTF_8));
  }
}