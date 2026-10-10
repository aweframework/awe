package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test of the real thing: a PostgreSQL container started by the utility and reached through the properties
 * the annotation writes. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresContainerSmokeTest.EmptyConfiguration.class)
@AweDatabaseTest(TestDatabase.POSTGRESQL)
class PostgresContainerSmokeTest {

  @Configuration
  static class EmptyConfiguration {
  }

  @Autowired
  private Environment environment;

  @Test
  void theAnnotationStartsAContainerAndTheContextCanQueryIt() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
      environment.getRequiredProperty("spring.datasource.url"),
      environment.getRequiredProperty("spring.datasource.username"),
      environment.getRequiredProperty("spring.datasource.password"));
         ResultSet result = connection.createStatement().executeQuery("select version()")) {
      assertThat(result.next()).isTrue();
      assertThat(result.getString(1)).startsWith("PostgreSQL");
    }
  }
}
