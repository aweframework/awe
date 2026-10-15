package com.almis.awe.service.data.builder.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlPaths}
 */
class SqlPathsTest {

  @Test
  @DisplayName("A node is an untyped path named after it")
  void nodeIsPath() {
    assertThat(SqlPaths.path("myTable")).hasToString("myTable")
      .extracting(path -> path.getType()).isEqualTo(Object.class);
  }

  @Test
  @DisplayName("A null node has no path")
  void nullNodeHasNoPath() {
    assertThat(SqlPaths.path(null)).isNull();
  }
}
