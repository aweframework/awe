package com.almis.awe.test.integration.security;

import com.almis.awe.dao.UserDAO;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the query that reads the permissions assigned to the profiles of a user (AweProPer)
 */
@Tag("integration")
@DisplayName("Profile permissions query tests")
class ProfilePermissionsQueryTest extends AbstractSpringAppIntegrationTest {

  private static final int FIRST_ID = 7720001;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private UserDAO userDAO;

  @AfterEach
  void deleteRows() throws Exception {
    execute("DELETE FROM AweProPer WHERE IdeAweProPer >= " + FIRST_ID);
  }

  @Test
  @DisplayName("The administrator profile has every permission and a profile without assignments has none")
  void administratorHasEveryPermission() {
    assertThat(userDAO.findPermissions(List.of("ADM"))).containsExactly("*");
    assertThat(userDAO.findPermissions(List.of("GNR"))).isEmpty();
  }

  @Test
  @DisplayName("The permissions of several profiles are read together, once each")
  void permissionsOfSeveralProfiles() throws Exception {
    execute("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) "
      + "SELECT " + FIRST_ID + ", IdePro, 'users.read' FROM AwePro WHERE Acr = 'GNR'");
    execute("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) "
      + "SELECT " + (FIRST_ID + 1) + ", IdePro, 'users.read' FROM AwePro WHERE Acr = 'OPE'");
    execute("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) "
      + "SELECT " + (FIRST_ID + 2) + ", IdePro, 'profiles.*' FROM AwePro WHERE Acr = 'OPE'");

    assertThat(userDAO.findPermissions(List.of("GNR", "OPE"))).containsExactly("profiles.*", "users.read");
  }

  @Test
  @DisplayName("Every permission of a profile is read, not only the first page of the query")
  void permissionsAreNotPaginated() throws Exception {
    int rows = 31;
    for (int i = 0; i < rows; i++) {
      execute("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) "
        + "SELECT " + (FIRST_ID + i) + ", IdePro, 'paged.p" + String.format("%02d", i) + "' FROM AwePro WHERE Acr = 'GNR'");
    }

    assertThat(userDAO.findPermissions(List.of("GNR"))).hasSize(rows);
    assertThat(userDAO.findAllPermissions().get("GNR")).hasSize(rows);
  }

  private void execute(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }
}
