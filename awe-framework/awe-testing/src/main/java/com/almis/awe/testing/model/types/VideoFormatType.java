package com.almis.awe.testing.model.types;

/**
 * Video formats recording enumeration
 */
public enum VideoFormatType {

  AVI (".avi"),
  WEBM (".webm");

  VideoFormatType(String videoExtension) {
    this.videoExtension = videoExtension;
  }

  private final String videoExtension;

  public String getVideoExtension() {
    return videoExtension;
  }

}
