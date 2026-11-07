package com.almis.awe.test.integration.security;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional profiles of a single sign-on user (AweOpeRol): at login AccessService launches
 * UpdateOauthUserAdditionalProfiles, which replaces the rows of the user with the profiles mapped from the provider
 * roles, audited in HISAweOpeRol. The user is a session parameter set only while the target runs.
 */
@Tag("integration")
@DisplayName("SSO additional profiles maintain tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class SsoAdditionalProfilesTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "ssorol932";
  private static final String USER = PREFIX + "usr";
  private static final String OTHER_USER = PREFIX + "oth";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private MaintainService maintainService;

  private SecurityAuditTestSupport support;
  private int userId;
  private int otherUserId;
  private int main;
  private int first;
  private int second;

  @BeforeEach
  void createUsersAndProfiles() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    deleteUsersAndProfiles();
    main = number("SELECT MAX(IdePro) FROM AwePro") + 1000;
    first = main + 1;
    second = main + 2;
    insertProfile(main, "Q8M", " main");
    insertProfile(first, "Q8A", " first");
    insertProfile(second, "Q8B", " second");
    userId = support.copyTestUser(USER);
    otherUserId = support.copyTestUser(OTHER_USER);
    execute("UPDATE ope SET IdePro = " + main + " WHERE IdeOpe IN (" + userId + ", " + otherUserId + ")");
  }

  @AfterEach
  void deleteUsersAndProfiles() throws Exception {
    String users = "SELECT IdeOpe FROM ope WHERE l1_nom LIKE '" + PREFIX + "%'";
    String profiles = "SELECT IdePro FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'";
    execute("DELETE FROM AweOpeRol WHERE IdeOpe IN (" + users + ") OR IdePro IN (" + profiles + ")");
    execute("DELETE FROM HISAweOpeRol WHERE IdeOpe IN (" + users + ") OR IdePro IN (" + profiles + ")");
    support.deleteUser(USER);
    support.deleteUser(OTHER_USER);
    execute("DELETE FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'");
  }

  @Test
  @DisplayName("The provider profiles replace the additional profiles of the SSO user, audited")
  void replacesTheAdditionalProfilesOfTheUser() throws Exception {
    insertAdditionalProfile(userId, second);
    insertAdditionalProfile(otherUserId, second);

    launchForUser(USER, first, second);

    assertThat(additionalProfiles(userId)).containsExactly(first, second);
    assertThat(additionalProfiles(otherUserId)).containsExactly(second);
    assertThat(number("SELECT COUNT(*) FROM HISAweOpeRol WHERE HISact = 'D' AND IdeOpe = " + userId)).isEqualTo(1);
    assertThat(number("SELECT COUNT(*) FROM HISAweOpeRol WHERE HISact = 'I' AND IdeOpe = " + userId)).isEqualTo(2);
  }

  @Test
  @DisplayName("Without other provider profiles the additional profiles of the SSO user are removed")
  void withoutProfilesRemovesTheAdditionalProfilesOfTheUser() throws Exception {
    insertAdditionalProfile(userId, first);
    insertAdditionalProfile(userId, second);

    launchForUser(USER);

    assertThat(additionalProfiles(userId)).isEmpty();
  }

  @Test
  @DisplayName("The user is never a request parameter: without the session parameter the target fails")
  void withoutTheSessionUserChangesNothing() throws Exception {
    insertAdditionalProfile(userId, second);
    ObjectNode parameters = profiles(first);
    parameters.put("userName", USER);
    parameters.put("user", USER);
    parameters.put(AccessService.SSO_PROFILES_USER_SESSION_PARAMETER, USER);

    assertThatThrownBy(() -> maintainService.launchPrivateMaintain(AccessService.UPDATE_OAUTH_ADDITIONAL_PROFILES,
      parameters)).isInstanceOf(AWException.class);

    assertThat(additionalProfiles(userId)).containsExactly(second);
  }

  private void launchForUser(String user, int... profileIds) throws Exception {
    AweSession session = context.getBean(AweSession.class);
    session.setParameter(AccessService.SSO_PROFILES_USER_SESSION_PARAMETER, user);
    try {
      maintainService.launchPrivateMaintain(AccessService.UPDATE_OAUTH_ADDITIONAL_PROFILES, profiles(profileIds));
    } finally {
      session.removeParameter(AccessService.SSO_PROFILES_USER_SESSION_PARAMETER);
    }
  }

  private ObjectNode profiles(int... profileIds) {
    ObjectNode parameters = objectMapper.createObjectNode();
    ArrayNode profiles = parameters.putArray("profiles");
    for (int profile : profileIds) {
      profiles.add(profile);
    }
    return parameters;
  }

  private void insertProfile(int id, String acronym, String name) throws Exception {
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + id + ", '" + acronym + "', '"
      + PREFIX + name + "', 1, NULL, NULL, 1)");
  }

  private void insertAdditionalProfile(int user, int profile) throws Exception {
    // Far beyond the OpeRolKey sequence, which the target uses for the new rows
    int id = Math.max(number("SELECT COALESCE(MAX(IdeAweOpeRol), 0) FROM AweOpeRol"),
      number("SELECT KeyVal FROM AweKey WHERE KeyNam = 'OpeRolKey'") + 1000) + 1;
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + id + ", " + user + ", " + profile + ")");
  }

  private List<Integer> additionalProfiles(int user) throws Exception {
    List<Integer> profiles = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT IdePro FROM AweOpeRol WHERE IdeOpe = " + user + " ORDER BY IdePro");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        profiles.add(result.getInt(1));
      }
    }
    return profiles;
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
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1);
    }
  }
}
