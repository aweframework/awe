package com.almis.awe.test.integration.database;

import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.FileService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the {@code saveUserAvatar} maintain target: it must exist, require authentication,
 * resolve the target user solely from the authenticated session (via
 * {@code UserSettingsService.saveUserAvatar}), and read the staged token from the request
 * parameter matching criterion id {@code CrtAvatar} (bound via
 * {@code <variable id="avatarToken" name="CrtAvatar"/>}) (Slice 6, tasks 35-36).
 */
@Tag("integration")
@DisplayName("Save user avatar maintain tests (stage-then-claim)")
class SaveUserAvatarMaintainTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private FileService fileService;

  @AfterEach
  void cleanUp() throws Exception {
    deleteUploadedAvatarFile("test");
    deleteUserSettingsRow("test");
  }

  private void deleteUploadedAvatarFile(String user) throws Exception {
    String token = getAvatarImage(user);
    if (token != null) {
      try {
        fileService.deleteFile(FileUtil.stringToFileData(token));
      } catch (Exception ignored) {
        // File may not exist for tests that only assert rejection.
      }
    }
  }

  @Test
  @WithAnonymousUser
  void unauthenticatedClaimIsRejected() throws Exception {
    mockMvc.perform(post("/action/maintain/saveUserAvatar")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtAvatar\":\"whatever\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @Transactional
  void authenticatedClaimResolvesToSessionUserAndPersistsToken() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionUser(session, "test");

    // Stage the file exactly like /file/upload would (real FileService call, real bytes on disk),
    // then claim the resulting token through the saveUserAvatar maintain. The token rides the
    // standard criterion-value channel: the request parameter key is the criterion id
    // ("CrtAvatar"), not the maintain variable id ("avatarToken"). Uses a filename distinct from
    // other avatar tests staging into the same "avatar" folder in the same test JVM, so a
    // concurrently running test's cleanup can never delete this test's staged file.
    MockMultipartFile stagedFile = new MockMultipartFile("file", "save-user-avatar-maintain-test.png", "image/png", realPngBytes());
    FileData stagedFileData = fileService.uploadFile(stagedFile, "avatar");
    String token = FileUtil.fileDataToString(stagedFileData);

    mockMvc.perform(post("/action/maintain/saveUserAvatar")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtAvatar\":\"" + token + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      // The home sidebar/navbar avatar (ButUsrAct) lives in the "base" view, separate from this
      // maintain's own "report" view, so a plain same-view refresh would never reach it. The
      // server must emit a client action explicitly addressed to the "base" view's avatar so it
      // reloads after a successful claim.
      .andExpect(jsonPath("$[?(@.type == 'filter')]").exists())
      .andExpect(jsonPath("$[?(@.type == 'filter')].address.view", contains("base")))
      .andExpect(jsonPath("$[?(@.type == 'filter')].address.component", contains("ButUsrAct")));

    assertEquals(token, getAvatarImage("test"));
  }

  private byte[] realPngBytes() throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return outputStream.toByteArray();
  }

  private void setSessionUser(MockHttpSession session, String user) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/session/set/user")
        .with(csrf())
        .param("value", user)
        .session(session))
      .andReturn();
    mvcResult.getResponse().getContentAsString();
  }

  private String getAvatarImage(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT AvatarImage FROM AweUserSettings WHERE Ope = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        return resultSet.next() ? resultSet.getString(1) : null;
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
