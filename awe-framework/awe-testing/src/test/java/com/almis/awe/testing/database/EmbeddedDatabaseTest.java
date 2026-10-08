package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real wiring without Docker: the annotation configures a Spring context that can open a connection.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EmbeddedDatabaseTest.EmptyConfiguration.class)
@AweDatabaseTest(TestDatabase.H2)
class EmbeddedDatabaseTest {

  @Configuration
  static class EmptyConfiguration {
  }

  @Autowired
  private Environment environment;

  @Test
  void theContextConnectsToTheConfiguredDatabase() throws SQLException {
    try (Connection connection = DriverManager.getConnection(
      environment.getRequiredProperty("spring.datasource.url"),
      environment.getRequiredProperty("spring.datasource.username"),
      environment.getProperty("spring.datasource.password", ""));
         ResultSet result = connection.createStatement().executeQuery("select 1")) {
      assertThat(result.next()).isTrue();
      assertThat(result.getInt(1)).isEqualTo(1);
    }
  }
}
