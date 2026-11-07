package com.almis.awe.test.integration.security;

import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the query that reads the additional profiles of a user (AweOpeRol) at login
 */
@Tag("integration")
@DisplayName("User additional profiles query tests")
class UserAdditionalProfilesQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "test";
  private static final int FIRST_ID = 7710001;
  private static final String INACTIVE_PROFILE = "QIN";
  private static final int FIRST_PROFILE_ID = 7730001;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private UserDAO userDAO;

  @AfterEach
  void deleteRows() throws Exception {
    execute("DELETE FROM AweOpeRol WHERE IdeAweOpeRol >= " + FIRST_ID);
    execute("DELETE FROM AwePro WHERE Acr = '" + INACTIVE_PROFILE + "' OR IdePro >= " + FIRST_PROFILE_ID);
  }

  @Test
  @DisplayName("A user without additional profiles has none")
  void userWithoutAdditionalProfiles() {
    assertThat(userDAO.findAdditionalProfiles(USER)).isEmpty();
  }

  @Test
  @DisplayName("The additional profiles of a user are read with their acronym, name and restriction, without the main one")
  void additionalProfilesWithoutTheMainOne() throws Exception {
    int user = number("SELECT IdeOpe FROM ope WHERE l1_nom = '" + USER + "'");
    int main = number("SELECT IdePro FROM ope WHERE l1_nom = '" + USER + "'");
    int other = number("SELECT MIN(IdePro) FROM AwePro WHERE IdePro <> " + main);
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + FIRST_ID + ", " + user + ", " + other + ")");
    // A row with the main profile is never expected, and is not read as an additional profile
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + (FIRST_ID + 1) + ", " + user + ", " + main + ")");

    assertThat(userDAO.findAdditionalProfiles(USER)).containsExactly(new UserProfile(
      text("SELECT Acr FROM AwePro WHERE IdePro = " + other),
      text("SELECT Nam FROM AwePro WHERE IdePro = " + other),
      text("SELECT Res FROM AwePro WHERE IdePro = " + other)));
  }

  @Test
  @DisplayName("Every additional profile of a user is read, not only the first page of the query")
  void additionalProfilesAreNotPaginated() throws Exception {
    int user = number("SELECT IdeOpe FROM ope WHERE l1_nom = '" + USER + "'");
    int profiles = 31;
    for (int i = 0; i < profiles; i++) {
      execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + (FIRST_PROFILE_ID + i)
        + ", 'Z" + String.format("%02d", i) + "', 'paged-profile-" + i + "', 1, NULL, 'general', 1)");
      execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + (FIRST_ID + i) + ", " + user + ", "
        + (FIRST_PROFILE_ID + i) + ")");
    }

    assertThat(userDAO.findAdditionalProfiles(USER)).hasSize(profiles);
  }

  @Test
  @DisplayName("An inactive profile is not read as an additional profile")
  void inactiveProfileIsNotRead() throws Exception {
    int user = number("SELECT IdeOpe FROM ope WHERE l1_nom = '" + USER + "'");
    int inactive = number("SELECT MAX(IdePro) FROM AwePro") + 1000;
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + inactive + ", '"
      + INACTIVE_PROFILE + "', 'inactive-profile', 1, NULL, 'general', 0)");
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + FIRST_ID + ", " + user + ", " + inactive
      + ")");

    assertThat(userDAO.findAdditionalProfiles(USER)).isEmpty();
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

  private int number(String sql) throws Exception {
    return Integer.parseInt(text(sql));
  }

  private String text(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      String value = resultSet.getString(1);
      return value == null ? null : value.trim();
    }
  }
}
