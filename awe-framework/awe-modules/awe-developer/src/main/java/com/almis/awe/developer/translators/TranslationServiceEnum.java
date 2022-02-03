package com.almis.awe.developer.translators;

public enum TranslationServiceEnum {
  /**
   * MyMemory Translate API service
   */
  MY_MEMORY("MyMemoryClient"),
  /**
   * MyMemory Translate API service using RAPID API
   */
  RAPID_API("RapidAPIClient");

  private final String value;

  TranslationServiceEnum(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}
