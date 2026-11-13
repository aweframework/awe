package com.almis.awe.service;

import com.almis.awe.model.type.ParameterType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Key filters of the multiple maintains ignore the case of the text variables
 */
class MaintainServiceMultipleKeyTest {

  @ParameterizedTest
  @EnumSource(value = ParameterType.class, names = {"STRING", "STRINGB", "STRINGL", "STRINGR", "STRING_HASH_RIPEMD160",
    "STRING_HASH_SHA", "STRING_HASH_PBKDF_2_W_HMAC_SHA_1", "STRING_HASH_PASSWORD", "STRING_ENCRYPT"})
  void textKeysIgnoreCase(ParameterType type) {
    assertThat(MaintainService.isCaseInsensitiveKey(type)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = ParameterType.class, names = {"STRINGN", "INTEGER", "LONG", "DATE", "TIMESTAMP", "BOOLEAN"})
  void otherKeysKeepCase(ParameterType type) {
    assertThat(MaintainService.isCaseInsensitiveKey(type)).isFalse();
  }
}
