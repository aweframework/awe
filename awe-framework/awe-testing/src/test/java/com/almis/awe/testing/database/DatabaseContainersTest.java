package com.almis.awe.testing.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseContainersTest {

  /** Hermetic: the tests never read the real environment variables */
  private static final Function<String, String> NO_ENVIRONMENT = name -> null;

  private String eulaBefore;

  @BeforeEach
  void rememberTheSettings() {
    eulaBefore = System.getProperty(TestDatabaseSettings.SQLSERVER_ACCEPT_EULA);
    System.clearProperty(TestDatabaseSettings.SQLSERVER_ACCEPT_EULA);
  }

  @AfterEach
  void restoreTheSettings() {
    restore(TestDatabaseSettings.SQLSERVER_ACCEPT_EULA, eulaBefore);
  }

  private static void restore(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }

  @Test
  void embeddedDatabasesHaveNoContainerConnection() {
    assertThat(DatabaseContainers.connectionFor(TestDatabase.H2, NO_ENVIRONMENT)).isEmpty();
    assertThat(DatabaseContainers.connectionFor(TestDatabase.HSQLDB, NO_ENVIRONMENT)).isEmpty();
  }

  @Test
  void sqlServerFailsWithoutAnExplicitEulaAcceptance() {
    assertThatThrownBy(() -> DatabaseContainers.connectionFor(TestDatabase.SQLSERVER, NO_ENVIRONMENT))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("EULA")
      .hasMessageContaining("awe.testing.sqlserver.accept-eula=true");
  }

  @Test
  void sqlServerEulaAcceptanceIsNeverImplied() {
    System.setProperty(TestDatabaseSettings.SQLSERVER_ACCEPT_EULA, "yes please");

    assertThat(TestDatabaseSettings.isSqlServerEulaAccepted(NO_ENVIRONMENT)).isFalse();
    System.setProperty(TestDatabaseSettings.SQLSERVER_ACCEPT_EULA, "true");
    assertThat(TestDatabaseSettings.isSqlServerEulaAccepted(NO_ENVIRONMENT)).isTrue();
  }

  @Test
  void theEnvironmentVariablesAreHonouredThroughTheInjectedLookup() {
    assertThat(TestDatabaseSettings.isSqlServerEulaAccepted(name -> "AWE_TESTING_SQLSERVER_ACCEPT_EULA".equals(name) ? "true" : null)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(value = TestDatabase.class, names = {"MYSQL", "POSTGRESQL", "SQLSERVER", "ORACLE"})
  void everyPinnedImageIsAcceptedByItsContainerModule(TestDatabase database) {
    // The container constructors check that the pinned image is compatible with the module without Docker;
    // getDockerImageName() would resolve the image and needs a Docker environment, so it is not called here
    assertThatCode(() -> DatabaseContainers.createContainer(database)).doesNotThrowAnyException();
  }
}
