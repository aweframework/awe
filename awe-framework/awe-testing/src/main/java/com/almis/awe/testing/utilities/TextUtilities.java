package com.almis.awe.testing.utilities;

public class TextUtilities {
  private TextUtilities() {};

  public static String sanitizeMessage(String message) {
    String messageSanitized = message
      .toLowerCase()
      .replaceAll("[\\W]+", "_")
      .replaceAll("_+", "_");
    return messageSanitized.length() > 80 ? messageSanitized.substring(0, 80) : messageSanitized;
  }
}
