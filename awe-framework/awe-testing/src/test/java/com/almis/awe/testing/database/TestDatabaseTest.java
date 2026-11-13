package com.almis.awe.testing.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TestDatabaseTest {

  @ParameterizedTest
  @EnumSource(value = TestDatabase.class, names = {"HSQLDB", "H2"})
  void embeddedDatabasesNeedNoContainer(TestDatabase database) {
    assertThat(database.needsContainer()).isFalse();
    assertThat(database.getImageKey()).isNull();
    assertThat(database.getEmbeddedUrl()).startsWith("jdbc:");
  }

  @ParameterizedTest
  @EnumSource(value = TestDatabase.class, names = {"MYSQL", "POSTGRESQL", "SQLSERVER", "ORACLE"})
  void serverDatabasesNeedAContainerWithAnImage(TestDatabase database) {
    assertThat(database.needsContainer()).isTrue();
    assertThat(database.getImageKey()).isNotBlank();
    assertThat(database.getEmbeddedUrl()).isNull();
  }

  @Test
  void scriptsFollowTheSqlFolderConvention() {
    assertThat(TestDatabase.HSQLDB.getSchemaScript()).isEqualTo("classpath:sql/schema-hsqldb.sql");
    assertThat(TestDatabase.H2.getDataScript()).isEqualTo("classpath:sql/testdata-h2.sql");
    assertThat(TestDatabase.ORACLE.getSchemaScript()).isEqualTo("classpath:sql/schema-oracledb.sql");
    assertThat(TestDatabase.SQLSERVER.getSchemaScript()).isEqualTo("classpath:sql/schema-sqlserverdb.sql");
    assertThat(TestDatabase.SQLSERVER.getDataScript()).isEqualTo("classpath:sql/testdata-sqlserverdb.sql");
    assertThat(TestDatabase.POSTGRESQL.getSchemaScript()).isEqualTo("classpath:sql/schema-postgresql.sql");
    assertThat(TestDatabase.POSTGRESQL.getDataScript()).isEqualTo("classpath:sql/testdata-postgresql.sql");
    // MySQL keeps its historical, irregular data script name
    assertThat(TestDatabase.MYSQL.getSchemaScript()).isEqualTo("classpath:sql/schema-mysqldb.sql");
    assertThat(TestDatabase.MYSQL.getDataScript()).isEqualTo("classpath:sql/testdata-mysql.sql");
  }

  @Test
  void everyDatabaseDeclaresItsJdbcDriver() {
    assertThat(TestDatabase.HSQLDB.getDriverClassName()).isEqualTo("org.hsqldb.jdbc.JDBCDriver");
    assertThat(TestDatabase.H2.getDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(TestDatabase.MYSQL.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
    assertThat(TestDatabase.POSTGRESQL.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(TestDatabase.SQLSERVER.getDriverClassName()).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    assertThat(TestDatabase.ORACLE.getDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
  }
}
