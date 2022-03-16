package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.util.security.Crypto;
import com.almis.awe.model.util.security.RipEmd160;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Optional;

/**
 * Encode Service Class
 * Encode Utilities for awe
 *
 * @author Pablo GARCIA - 17/MAR/2011
 */
@Slf4j
public class EncodeService extends ServiceConfig {

  private static final String STRING_ENCODE_ERROR = "String encode error";
  private final StringKeyGenerator keyGenerator = KeyGenerators.string();

  // Autowire beans
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Static class to store hashing algorithms to make the call to these algorithms more understandable.
   */
  public static final class HashingAlgorithms {

    /**
     * Private constructor to enclose the default one
     */
    private HashingAlgorithms() {
    }

    // HASHING ALGORITHMS
    public static final String MD5 = "MD5";
    public static final String SHA_256 = "SHA-256";
    public static final String SHA_512 = "SHA-512";
  }

  /**
   * Hide the constructor
   * @param baseConfigProperties Base config properties
   * @param securityConfigProperties Security config properties
   */
  public EncodeService(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Encodes a text in RIPEMD-160
   *
   * @param text Text to encode
   * @return Text encoded
   */
  public static String encodeRipEmd160(String text) {

    /* Variable definition */
    RipEmd160 encoder = new RipEmd160();
    return encoder.encodeRipEmd160(text);
  }

  /**
   * Decrypts a string with RipEmd160
   *
   * @param text String to be decrypted
   * @return String decrypted
   * @throws AWException Error in decryption
   */
  public String decryptRipEmd160(String text) throws AWException {
    return decryptRipEmd160WithPhraseKey(text, securityConfigProperties.getMasterKey(), baseConfigProperties.getEncoding());
  }

  /**
   * Encrypts a string with RipEmd160
   *
   * @param text String to be encrypted
   * @return String encrypted
   * @throws AWException Error in encryption
   */
  public String encryptRipEmd160(String text) throws AWException {
    return encryptRipEmd160WithPhraseKey(text, securityConfigProperties.getMasterKey(), baseConfigProperties.getEncoding());
  }

  /**
   * Encrypts a string with RipEmd160 and phraseKey
   *
   * @param text      String to be encrypted
   * @param phraseKey Phrase key
   * @return String encrypted
   * @throws AWException Error in encryption
   */
  public String encryptRipEmd160WithPhraseKey(String text, String phraseKey) throws AWException {
    return encryptRipEmd160WithPhraseKey(text, phraseKey, baseConfigProperties.getEncoding());
  }


  /**
   * Encrypts a string with RipEmd160 and phraseKey
   *
   * @param text      String to be encrypted
   * @param phraseKey Phrase key
   * @param encoding  Encoding
   * @return String encrypted
   * @throws AWException Error in encryption
   */
  public String encryptRipEmd160WithPhraseKey(String text, String phraseKey, String encoding) throws AWException {
    try {
      String key = Optional.ofNullable(phraseKey).filter(StringUtils::isNotEmpty).orElse(securityConfigProperties.getMasterKey());
      RipEmd160 encoder = new RipEmd160(key);
      return encoder.encrypt(text, encoding);
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Decrypts a string with RipEmd160 and phraseKey
   *
   * @param text      String to be decrypted
   * @param phraseKey Phrase key
   * @param encoding  Encoding
   * @return String decrypted
   * @throws AWException Error in decryption
   */
  public String decryptRipEmd160WithPhraseKey(String text, String phraseKey, String encoding) throws AWException {
    try {
      String key = Optional.ofNullable(phraseKey).filter(StringUtils::isNotEmpty).orElse(securityConfigProperties.getMasterKey());
      RipEmd160 encoder = new RipEmd160(key);
      return encoder.decrypt(text, encoding);
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Decrypts a string with Aes
   *
   * @param text String to be decrypted
   * @return String decrypted
   * @throws AWException Error in decryption
   */
  public String decryptAes(String text) throws AWException {
    return decryptAes(text, securityConfigProperties.getMasterKey());
  }

  /**
   * Decrypts a string with Aes
   *
   * @param text     String to be decrypted
   * @param password Password
   * @return String decrypted
   * @throws AWException Error in decryption
   */
  public String decryptAes(String text, String password) throws AWException {
    try {
      return Crypto.AES.decrypt(text, password, baseConfigProperties.getEncoding());
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Encrypts a string with Aes
   *
   * @param text String to be encrypted
   * @return String encrypted
   * @throws AWException Error in encryption
   */
  public String encryptAes(String text) throws AWException {
    try {
      return Crypto.AES.encrypt(text, securityConfigProperties.getMasterKey(), baseConfigProperties.getEncoding());
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Encrypts a string with Aes
   *
   * @param text     String to be encrypted
   * @param password Password
   * @return String encrypted
   * @throws AWException Error in encryption
   */
  public String encryptAes(String text, String password) throws AWException {
    try {
      return Crypto.AES.encrypt(text, password, baseConfigProperties.getEncoding());
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Encodes a text in DES (Symmetric)
   *
   * @param encodeValue Text to encode
   * @return Text encoded
   * @throws AWException Error in encoding
   */
  public static String encodeSymmetric(byte[] encodeValue) throws AWException {
    try {
      return base64Encode(encodeValue);
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Encodes a text in DES (Symmetric)
   *
   * @param text Text to encode
   * @return Text encoded
   * @throws AWException Error in encoding
   */
  public static String encodeSymmetric(String text) throws AWException {
    return encodeSymmetric(text.getBytes());
  }

  /**
   * Decodes a text in DES (Symmetric)
   *
   * @param text Text to decode
   * @return Text decoded
   * @throws AWException Error in decoding
   */
  public String decodeSymmetric(String text) throws AWException {
    try {
      return new String(decodeSymmetricAsByteArray(text), baseConfigProperties.getEncoding());
    } catch (UnsupportedEncodingException exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Decodes a text in DES (Symmetric)
   *
   * @param text Text to decode
   * @return Text decoded
   * @throws AWException Error in decoding
   */
  public byte[] decodeSymmetricAsByteArray(String text) throws AWException {
    try {
      return base64DecodeAsByteArray(Optional.ofNullable(text).orElse(""));
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Encodes a text in Hex
   *
   * @param text Text to encode
   * @return Text encoded
   * @throws AWException Error encoding
   */
  public String encodeHex(String text) throws AWException {
    try {
      return new String(new Hex().encode(text.getBytes()), baseConfigProperties.getEncoding());
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Decodes a text in Hex
   *
   * @param text Text to decode
   * @return Text decoded
   * @throws AWException Error decoding
   */
  public String decodeHex(String text) throws AWException {
    try {
      return new String(new Hex().decode(Optional.ofNullable(text).orElse("").getBytes(StandardCharsets.UTF_8)), baseConfigProperties.getEncoding());
    } catch (Exception exc) {
      throw new AWException(exc.getClass().getSimpleName(), exc.toString(), exc);
    }
  }

  /**
   * Decodes a text in DES (Symmetric)
   *
   * @param text   Text to decode
   * @param active Encoding enabled
   * @return Text decoded
   * @throws AWException Error encoding
   */
  public String encodeTransmission(String text, boolean active) throws AWException {
    return active ? encodeSymmetric(text) : text;
  }

  /**
   * Decodes a text in DES (Symmetric)
   *
   * @param text   Text to decode
   * @param active Encoding enabled
   * @return Text decoded
   * @throws AWException Error decoding
   */
  public String decodeTransmission(String text, boolean active) throws AWException {
    return active ? decodeSymmetric(text) : text;
  }

  /**
   * Hashes a String with an SHA-X algorithm without any salt, where X is the algorithm given in the algorithm variable
   *
   * @param algorithm Algorithm
   * @param text      Text to hash
   * @return String Hexadecimal format of the output bytearray
   * @throws AWException Error hashing
   */
  public String hash(String algorithm, String text) throws AWException {
    return hash(algorithm, text, null);
  }

  /* HASHING ALGORITHMS */

  /**
   * Hashes a String with an SHA-X algorithm with a salt, where X is the algorithm given in the algorithm variable
   *
   * @param algorithm Algorithm
   * @param text      Text to hash
   * @param salt      The salt to be applied to the algorithm
   * @return String Hexadecimal format of the output bytearray
   * @throws AWException Error hashing
   */
  public String hash(String algorithm, String text, String salt) throws AWException {
    return Crypto.HASH.hash(text, algorithm, salt, Charset.forName(baseConfigProperties.getEncoding()));
  }

  /**
   * Hashes a String with a PBKDF2 algorithm based on SHA-1
   *
   * @param text Text to encode
   * @return String in Hexadecimal format
   * @throws AWException Error encoding
   */
  public String encodePBKDF2WithHmacSHA1(String text) throws AWException {
    final int keyLength = 256;
    return encodePBKDF2WithHmacSHA1(text, securityConfigProperties.getMasterKey(), Crypto.Utils.getRecommendedIterationNumber(), keyLength);
  }

  /**
   * Hashes a String with a PBKDF2 algorithm based on SHA-1
   *
   * @param text Text to encode
   * @param salt should be of about 160 bit size or more to create a strong salt
   * @return String in Hexadecimal format
   * @throws AWException Error encoding
   */
  public String encodePBKDF2WithHmacSHA1(String text, String salt) throws AWException {
    final int keyLength = 256;
    return encodePBKDF2WithHmacSHA1(text, salt, Crypto.Utils.getRecommendedIterationNumber(), keyLength);
  }

  /**
   * Hashes a String with a PBKDF2 algorithm based on SHA-1
   *
   * @param text       Text to encode
   * @param salt       should be of about 160 bit size or more to create a strong salt
   * @param iterations Number of iterations
   * @return String in Hexadecimal format
   * @throws AWException Error encoding
   */
  public String encodePBKDF2WithHmacSHA1(String text, String salt, int iterations) throws AWException {
    final int keyLength = 256;
    return encodePBKDF2WithHmacSHA1(text, salt, iterations, keyLength);
  }

  /**
   * Hashes a String with a PBKDF2 algorithm based on SHA-1
   *
   * @param text       Text to encode
   * @param salt       should be of about 160 bit size or more to create a strong salt
   * @param iterations recommended value is 256000 in 2016, double this value every 2 years
   * @param keyLength  should not be bigger than the maximum output length of the SHA-1 algorithm which is 160 bit (40 hex characters)
   * @return String in Hexadecimal format
   * @throws AWException Error encoding
   */

  public String encodePBKDF2WithHmacSHA1(String text, String salt, int iterations, int keyLength) throws AWException {
    try {
      // Get instance of the hashing algorithm
      SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

      // Add parameters
      PBEKeySpec spec = new PBEKeySpec(text.toCharArray(), salt.getBytes(baseConfigProperties.getEncoding()), iterations, keyLength);

      // Generate a key for the hash
      SecretKey key = skf.generateSecret(spec);

      // Hash
      byte[] res = key.getEncoded();

      // Get the hashed value as hexadecimal string
      return Crypto.Utils.encodeHex(res);
    } catch (NoSuchAlgorithmException exc) {
      throw new AWException(STRING_ENCODE_ERROR, "The algorithm does not exist.", exc);
    } catch (InvalidKeySpecException exc) {
      throw new AWException(STRING_ENCODE_ERROR, "The specified key is not valid or is not long enough.", exc);
    } catch (UnsupportedEncodingException exc) {
      throw new AWException(STRING_ENCODE_ERROR, "The specified encoding is not valid.", exc);
    }
  }

  /* *************************************************** */
  /* *********** Secure Random Methods ***************** */
  /* *************************************************** */

  /**
   * generate secure random object
   *
   * @return Secure random
   */
  public static SecureRandom getSecureRandom() {
    try {
      return SecureRandom.getInstance(AweConstants.RANDOM_ALGORITHM);
    } catch (NoSuchAlgorithmException exc) {
      log.error("Selected algorithm does not exist: {}", AweConstants.RANDOM_ALGORITHM, exc);
      return new SecureRandom();
    }
  }

  /**
   * Create a secure random string of the given length with the alphabet
   *
   * @return Random string
   */
  public String getSecureRandomString() {
    return keyGenerator.generateKey();
  }

  /**
   * Encode given text to the given encoding
   *
   * @param text     Text to encode
   * @param encoding Encoding
   * @return Text encoded
   * @throws UnsupportedEncodingException Error encoding
   */
  public static String encodeLanguage(String text, String encoding) throws UnsupportedEncodingException {
    return new String(text.getBytes(), encoding);
  }

  /**
   * Symmetric encryption
   *
   * @param digest    Digest
   * @param valueEnc  Value to encode
   * @param secretKey Key
   * @return Value encoded
   */
  public String encrypt(String digest, String valueEnc, final String secretKey) {

    try {
      SecretKey key = generateKeyFromString(digest, secretKey);
      final Cipher c = Cipher.getInstance(digest);

      byte[] ivBytes = new byte[16];
      getSecureRandom().nextBytes(ivBytes);
      IvParameterSpec iv = new IvParameterSpec(ivBytes);

      c.init(Cipher.ENCRYPT_MODE, key, iv);
      final byte[] encValue = c.doFinal(valueEnc.getBytes());
      return base64Encode(encValue);
    } catch (Exception ex) {
      log.error(ex.getLocalizedMessage());
    }
    return null;
  }

  /**
   * Symmetric decryption
   *
   * @param digest         Digest
   * @param encryptedValue Encrypted value
   * @param secretKey      Key
   * @return Decrypted value
   */
  public String decrypt(String digest, String encryptedValue, final String secretKey) {

    String decryptedValue = null;

    try {
      SecretKey key = generateKeyFromString(digest, secretKey);
      final Cipher c = Cipher.getInstance(digest);

      byte[] ivBytes = new byte[16];
      getSecureRandom().nextBytes(ivBytes);
      IvParameterSpec iv = new IvParameterSpec(ivBytes);

      c.init(Cipher.DECRYPT_MODE, key, iv);
      final byte[] decorVal = base64Decode(encryptedValue).getBytes();
      final byte[] decValue = c.doFinal(decorVal);
      decryptedValue = new String(decValue, baseConfigProperties.getEncoding());
    } catch (Exception ex) {
      log.error(ex.getLocalizedMessage());
    }

    return decryptedValue;
  }

  /**
   * Generate valid key from user given string
   *
   * @param algorithm Algorithm
   * @param secKey    Secret key
   * @return Secret key
   * @throws NoSuchAlgorithmException Invalid algorithm
   * @throws InvalidKeySpecException  Error retrieving key
   */
  private SecretKey generateKeyFromString(String algorithm, final String secKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    KeySpec spec = new PBEKeySpec(secKey.toCharArray(), base64Encode(secKey).getBytes(), 65536, 256);
    SecretKey key = factory.generateSecret(spec);
    return new SecretKeySpec(key.getEncoded(), algorithm);
  }

  /**
   * Base64 encode string
   *
   * @param text Text to encode
   * @return Text encoded
   */
  public static String base64Encode(String text) {
    return Base64.encodeBase64String(text.getBytes());
  }

  /**
   * Base64 encode byte array
   *
   * @param text Text to encode
   * @return Text encoded
   */
  public static String base64Encode(byte[] text) {
    return Base64.encodeBase64URLSafeString(text);
  }

  /**
   * Base64 decode string
   *
   * @param text Text to decode
   * @return Text decoded
   * @throws java.io.UnsupportedEncodingException unsupported encoding exception
   */
  public String base64Decode(String text) throws UnsupportedEncodingException {
    return new String(Base64.decodeBase64(text.getBytes()), baseConfigProperties.getEncoding());
  }

  /**
   * Base64 decode string
   *
   * @param text Text to decode
   * @return Text decoded
   */
  public byte[] base64DecodeAsByteArray(String text) {
    return Base64.decodeBase64(text);
  }

  /**
   * Base64 decode byte array
   *
   * @param text Text to decode
   * @return Text decoded
   * @throws java.io.UnsupportedEncodingException unsupported encoding exception
   */
  public String base64Decode(byte[] text) throws UnsupportedEncodingException {
    return new String(Base64.decodeBase64(text), baseConfigProperties.getEncoding());
  }
}
