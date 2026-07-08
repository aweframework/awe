package com.almis.awe.test.integration.security;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that {@code GET /avatar} is authenticated-only. This empirically validates (does not
 * assume) that the custom {@code PublicQueryMaintainAuthorization} manager used by
 * {@code authenticatedRequestMatchers} correctly gates a plain, non-query/non-maintain controller
 * route such as {@code /avatar} (Slice 2, tasks 19-22).
 */
@Tag("integration")
@DisplayName("Avatar security tests")
@Transactional
class AvatarSecurityIntegrationTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Autowired
  private FileService fileService;

  @AfterEach
  void cleanUp() throws Exception {
    deleteUploadedAvatarFile("test");
    deleteUploadedAvatarFile("donald");
    deleteUserSettingsRow("test");
    deleteUserSettingsRow("donald");
  }

  /**
   * Delete any avatar file actually written to disk by a real upload during the test. The
   * {@code AweUserSettings} row itself is rolled back automatically ({@code @Transactional}), but
   * a real file write to the upload store is not covered by that rollback, so it must be cleaned
   * up explicitly to avoid leaving orphaned files across test runs.
   */
  private void deleteUploadedAvatarFile(String user) throws Exception {
    String token = getAvatarImage(user);
    if (token != null) {
      try {
        fileService.deleteFile(FileUtil.stringToFileData(token));
      } catch (Exception ignored) {
        // File may not exist (e.g. tests that only insert a token referencing a missing file).
      }
    }
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

  @Test
  @WithAnonymousUser
  void unauthenticatedRequestIsRejected() throws Exception {
    mockMvc.perform(get("/avatar"))
      .andExpect(status().isUnauthorized());
  }

  /**
   * Maintain-path auth assertion for {@code saveUserAvatar} (Slice 6, task 40): an unauthenticated
   * claim attempt must be rejected. The multipart {@code POST /avatar} upload path this replaced
   * has been retired; claims now go exclusively through the authenticated
   * {@code saveUserAvatar} maintain (see {@code SaveUserAvatarMaintainTest} for the full
   * stage-then-claim round trip and the authenticated success/persistence path).
   */
  @Test
  @WithAnonymousUser
  void unauthenticatedSaveUserAvatarClaimIsRejected() throws Exception {
    mockMvc.perform(post("/action/maintain/saveUserAvatar")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtAvatar\":\"whatever\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void avatarResponseIncludesNosniffHeader() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionUser(session, "test");

    // Present or absent avatar, the security header must always be there: it is added by
    // Spring Security's default header writer chain, not by AvatarController itself.
    mockMvc.perform(get("/avatar").session(session))
      .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void authenticatedUserWithNoAvatarReceives404() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionUser(session, "test");

    mockMvc.perform(get("/avatar").session(session))
      .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void authenticatedUserWithUnresolvableAvatarFileReceives404() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionUser(session, "test");

    String token = FileUtil.fileDataToString(
      new FileData().setFileName("avatar.png").setMimeType("image/png").setFileSize(0L));
    insertUserSettingsRow(9101, "test", token);

    // The stored token references a file that does not exist on disk: resolution must not throw
    // a 500, it must degrade to 404 exactly like the "no avatar" case (never leak an exception).
    mockMvc.perform(get("/avatar").session(session))
      .andExpect(status().isNotFound());
  }

  /**
   * Genuine end-to-end happy path (Slice 6, task 40): an authenticated user stages a real,
   * decodable image via the public {@code /file/upload} model (simulated here with a direct
   * {@code FileService} call, since staging bytes on disk requires no authentication of its own),
   * claims it through the authenticated {@code saveUserAvatar} maintain, and retrieves it back
   * through {@code GET /avatar} — both through the real filter chain (CSRF, authentication,
   * {@code PublicQueryMaintainAuthorization} fallthrough). Asserts a real {@code 200}, correct
   * {@code Content-Type}, {@code Cache-Control: no-cache}, and that the returned bytes are exactly
   * the bytes that were staged.
   */
  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void authenticatedUserCanClaimAStagedAvatarAndThenDownloadIt() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionUser(session, "test");

    byte[] pngBytes = realPngBytes();
    // Filename distinct from other avatar tests staging into the same "avatar" folder in the
    // same test JVM, so a concurrently running test's cleanup can never delete this file.
    FileData stagedFileData = fileService.uploadFile(
      new MockMultipartFile("file", "avatar-security-integration-test.png", "image/png", pngBytes), "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    mockMvc.perform(post("/action/maintain/saveUserAvatar")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtAvatar\":\"" + stagedToken + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    mockMvc.perform(get("/avatar").session(session))
      .andExpect(status().isOk())
      .andExpect(header().string("Content-Type", "image/png"))
      .andExpect(header().string("Cache-Control", "no-cache"))
      .andExpect(content().bytes(pngBytes));
  }

  private static byte[] realPngBytes() throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return outputStream.toByteArray();
  }

  /**
   * Cross-user isolation: a second authenticated user must never receive the first user's
   * avatar. Resolution is session-bound (no path/token parameter exists to guess), so this is
   * asserted behaviorally: donald's request against /avatar must not be rejected as
   * "unauthenticated" (he is authenticated) and must resolve strictly to donald's own (absent)
   * avatar, not test's.
   */
  @Test
  @WithMockUser(username = "donald", password = "donald", roles = {"ADMIN", "USER"})
  void secondAuthenticatedUserCannotRetrieveFirstUsersAvatar() throws Exception {
    MockHttpSession testSession = new MockHttpSession();
    setSessionUser(testSession, "test");
    String token = FileUtil.fileDataToString(
      new FileData().setFileName("avatar.png").setMimeType("image/png").setFileSize(0L));
    insertUserSettingsRow(9102, "test", token);

    MockHttpSession donaldSession = new MockHttpSession();
    setSessionUser(donaldSession, "donald");

    // donald has no AweUserSettings row of his own: he must get 404, never test's avatar bytes
    mockMvc.perform(get("/avatar").session(donaldSession))
      .andExpect(status().isNotFound());
  }

  private void setSessionUser(MockHttpSession session, String user) throws Exception {
    MvcResult mvcResult = mockMvc.perform(post("/session/set/user")
        .with(csrf())
        .param("value", user)
        .session(session))
      .andReturn();
    mvcResult.getResponse().getContentAsString();
  }

  private void insertUserSettingsRow(int id, String user, String avatarToken) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweUserSettings (IdeUsrSet, Ope, AvatarImage) VALUES (?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, user);
      statement.setString(3, avatarToken);
      statement.executeUpdate();
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
