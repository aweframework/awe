package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseImagesTest {

  @ParameterizedTest
  @EnumSource(value = TestDatabase.class, names = {"MYSQL", "POSTGRESQL", "SQLSERVER", "ORACLE"})
  void everyServerDatabaseImageIsPinnedByTagAndDigest(TestDatabase database) {
    assertThat(TestDatabaseImages.imageFor(database))
      .matches("[a-z0-9./_-]+:[A-Za-z0-9._-]+@sha256:[a-f0-9]{64}");
  }

  @ParameterizedTest
  @EnumSource(value = TestDatabase.class, names = {"MYSQL", "POSTGRESQL", "SQLSERVER", "ORACLE"})
  void testcontainersPullsThePinnedDigest(TestDatabase database) {
    String image = TestDatabaseImages.imageFor(database);
    String digest = image.substring(image.indexOf('@') + 1);

    assertThat(DockerImageName.parse(image).asCanonicalNameString()).contains(digest);
  }

  @Test
  void embeddedDatabasesHaveNoImage() {
    assertThatThrownBy(() -> TestDatabaseImages.imageFor(TestDatabase.H2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("H2")
      .hasMessageContaining("embedded");
  }

  @Test
  void aMissingImageEntryNamesTheKeyAndTheFile() {
    assertThatThrownBy(() -> TestDatabaseImages.lookup("unknown"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("awe.testing.image.unknown")
      .hasMessageContaining("awe-testing-images.properties");
  }
}
