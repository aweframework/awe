package com.almis.awe.testing.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Container images of the test databases, read from {@code awe-testing-images.properties}.
 *
 * <p>Every image is pinned as {@code name:tag@sha256:digest}; Renovate keeps the file up to date.</p>
 */
public final class TestDatabaseImages {

  static final String RESOURCE = "awe-testing-images.properties";
  static final String KEY_PREFIX = "awe.testing.image.";

  private TestDatabaseImages() {
  }

  /**
   * Get the pinned image of a database that runs in a container
   *
   * @param database Database
   * @return Image reference
   */
  public static String imageFor(TestDatabase database) {
    if (!database.needsContainer()) {
      throw new IllegalArgumentException(database + " is an embedded database and has no container image");
    }
    return lookup(database.getImageKey());
  }

  static String lookup(String key) {
    String image = load().getProperty(KEY_PREFIX + key);
    if (image == null || image.isBlank()) {
      throw new IllegalStateException("No image '" + KEY_PREFIX + key + "' found in " + RESOURCE);
    }
    return image.trim();
  }

  private static Properties load() {
    Properties properties = new Properties();
    try (InputStream stream = TestDatabaseImages.class.getClassLoader().getResourceAsStream(RESOURCE)) {
      if (stream == null) {
        throw new IllegalStateException("Resource " + RESOURCE + " not found in the classpath");
      }
      properties.load(stream);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot read " + RESOURCE, exception);
    }
    return properties;
  }
}
