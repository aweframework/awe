package com.almis.awe.test.integration.database;

import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the {@code upsertUserAvatar} maintain target: insert-or-update semantics for
 * {@code AweUserSettings}, one row per user (Slice 2, tasks 10-12).
 */
@Tag("integration")
@DisplayName("User settings maintain tests (avatar upsert)")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
@Transactional
class UserSettingsMaintainTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private DataSource dataSource;

  private MockHttpSession session;

  @BeforeEach
  void setUp() throws Exception {
    session = new MockHttpSession();
    setSessionUser("test");
  }

  @AfterEach
  void cleanUp() throws Exception {
    deleteUserSettingsRow("test");
  }

  /**
   * When no AweUserSettings row exists for the session user, upsertUserAvatar must create
   * exactly one row with IdeUsrSet from nextIdForUserSettings, Ope from the session user, and
   * the given token.
   */
  @Test
  void testUpsertUserAvatarInsertsRowWhenAbsent() throws Exception {
    String token = FileUtil.fileDataToString(
      new com.almis.awe.model.dto.FileData().setFileName("avatar.png").setMimeType("image/png"));

    launchUpsertUserAvatar(token);

    assertEquals(1, countUserSettingsRows("test"));
    assertEquals(token, getAvatarImage("test"));
  }

  /**
   * When a row already exists for the session user, upsertUserAvatar must update AvatarImage on
   * the existing row rather than inserting a duplicate.
   */
  @Test
  void testUpsertUserAvatarUpdatesExistingRow() throws Exception {
    String firstToken = FileUtil.fileDataToString(
      new com.almis.awe.model.dto.FileData().setFileName("first.png").setMimeType("image/png"));
    String secondToken = FileUtil.fileDataToString(
      new com.almis.awe.model.dto.FileData().setFileName("second.png").setMimeType("image/png"));

    launchUpsertUserAvatar(firstToken);
    launchUpsertUserAvatar(secondToken);

    assertEquals(1, countUserSettingsRows("test"));
    assertEquals(secondToken, getAvatarImage("test"));
  }

  private void launchUpsertUserAvatar(String avatarToken) throws Exception {
    mockMvc.perform(post("/action/maintain/upsertUserAvatar")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"avatarToken\":\"" + avatarToken + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private void setSessionUser(String user) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/session/set/user")
        .with(csrf())
        .param("value", user)
        .session(session))
      .andReturn();
    mvcResult.getResponse().getContentAsString();
  }

  private int countUserSettingsRows(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT COUNT(*) FROM AweUserSettings WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private String getAvatarImage(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT AvatarImage FROM AweUserSettings WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    }
  }

  private void deleteUserSettingsRow(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "DELETE FROM AweUserSettings WHERE Ope = ?")) {
      statement.setString(1, user);
      statement.executeUpdate();
    }
  }
}
