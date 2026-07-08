package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.file.FileUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

  @InjectMocks
  private UserSettingsService userSettingsService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private QueryService queryService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private FileService fileService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    userSettingsService.setApplicationContext(applicationContext);
  }

  private void mockCurrentUser(String user) {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.getUser()).thenReturn(user);
  }

  private void mockLocale() {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenReturn("locale");
  }

  private DataList dataListWithToken(String token) {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put("avatarToken", new CellData(token));
    dataList.addRow(row);
    return dataList;
  }

  /**
   * Real, decodable PNG bytes (not just a declared content-type) for tests that must pass
   * server-side content validation.
   */
  private static byte[] realPngBytes() throws Exception {
    BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);
    return outputStream.toByteArray();
  }

  /**
   * A real, well-formed, decodable 1x1 lossy WebP file (RIFF/WEBP/VP8 container, valid per the
   * WebP spec). Used to prove the MIME allow-list is what rejects WebP now, not an unrelated
   * "undecodable bytes" failure: stock JDK 17 {@link ImageIO} has no WebP reader at all
   * ({@code ImageIO.getReaderMIMETypes()} lists png/jpeg/gif/bmp/tiff/wbmp/x-png only, never
   * webp), so even this genuinely valid WebP file cannot be decoded by {@code ImageIO.read}.
   */
  private static byte[] realWebpBytes() {
    return new byte[] {
      'R', 'I', 'F', 'F', 0x26, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P',
      'V', 'P', '8', ' ', 0x1a, 0x00, 0x00, 0x00,
      0x30, 0x01, 0x00, (byte) 0x9d, 0x01, 0x2a, 0x01, 0x00, 0x01, 0x00,
      0x02, 0x00, 0x34, 0x25, (byte) 0xa4, 0x00, 0x03, 0x70, 0x00, (byte) 0xfe,
      (byte) 0xfd, 0x50, 0x00, 0x00, 0x00
    };
  }

  @Test
  void getAvatarForCurrentUserReturnsEmptyWhenNoToken() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));

    Optional<FileData> result = userSettingsService.getAvatarForCurrentUser();

    assertTrue(result.isEmpty());
  }

  @Test
  void getAvatarForCurrentUserReturnsDecodedFileDataWhenTokenPresent() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

    String token = FileUtil.fileDataToString(new FileData("avatar.png", 1024L, "image/png"));
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataListWithToken(token)));

    Optional<FileData> result = userSettingsService.getAvatarForCurrentUser();

    assertTrue(result.isPresent());
    assertEquals("avatar.png", result.get().getFileName());
    assertEquals("image/png", result.get().getMimeType());
  }

  @Test
  void upsertUserAvatarFallsBackToUpdateWhenInsertRaceLosesToConcurrentInsert() throws Exception {
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    // Read-before-write sees no row (classic TOCTOU): another request wins the race and inserts
    // first, so our own insert then violates the UNIQUE(Ope) constraint.
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(eq("insertUserAvatar"), any(ObjectNode.class)))
      .thenThrow(new AWException("Error launching maintain", "Duplicate key value violates unique constraint"));
    when(maintainService.launchPrivateMaintain(eq("updateUserAvatar"), any(ObjectNode.class)))
      .thenReturn(new ServiceData());

    ServiceData result = userSettingsService.upsertUserAvatar("test", "token");

    assertNotNull(result);
    verify(maintainService, times(1)).launchPrivateMaintain(eq("insertUserAvatar"), any(ObjectNode.class));
    verify(maintainService, times(1)).launchPrivateMaintain(eq("updateUserAvatar"), any(ObjectNode.class));
  }

  // --- saveUserAvatar(String avatarToken) — stage-then-claim (Slice 6, task 32) ---
  // Migrated from the retired uploadAvatar(MultipartFile) tests (Slice 6, task 40).

  /**
   * Write the given bytes to a real file under a temp "staged" folder and return a
   * {@link FileData} pointing at it, mimicking what {@code /file/upload} would have staged on
   * disk. {@code saveUserAvatar}'s validators read the staged bytes from disk (via
   * {@code fileService.getFullPath}), so the file must actually exist for content validation.
   */
  private FileData stageFile(String fileName, String mimeType, byte[] bytes) throws Exception {
    java.nio.file.Path stagedDir = java.nio.file.Files.createTempDirectory("avatar-staged");
    java.nio.file.Files.write(stagedDir.resolve(fileName), bytes);
    FileData fileData = new FileData(fileName, (long) bytes.length, mimeType, "avatar");
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);
    when(fileService.getFullPath(argThat(fd -> fd != null && fileName.equals(fd.getFileName())), eq(false)))
      .thenReturn(stagedDir.toString() + java.io.File.separator);
    return fileData;
  }

  @Test
  void saveUserAvatarAcceptsAllowedMimeTypeWithinSizeLimitAndDelegatesToUpsert() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    FileData stagedFileData = stageFile("avatar.png", "image/png", realPngBytes());
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    userSettingsService.saveUserAvatar(stagedToken);

    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  /**
   * The home sidebar/navbar avatar ({@code ButUsrAct}) lives in the "base" view, while
   * {@code saveUserAvatar} runs from the user-settings modal ("report" view). A plain
   * same-view refresh would never reach it, so the server must emit a client action addressed
   * explicitly to the "base" view's avatar component so the home avatar reloads after a save.
   */
  @Test
  void saveUserAvatarReturnsServiceDataWithFilterClientActionAddressedToBaseAvatar() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    FileData stagedFileData = stageFile("avatar.png", "image/png", realPngBytes());
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    ServiceData result = userSettingsService.saveUserAvatar(stagedToken);

    assertNotNull(result);
    assertEquals(1, result.getClientActionList().size());
    ClientAction clientAction = result.getClientActionList().get(0);
    assertEquals("filter", clientAction.getType());
    assertNotNull(clientAction.getAddress());
    assertEquals("base", clientAction.getAddress().getView());
    assertEquals("ButUsrAct", clientAction.getAddress().getComponent());
  }

  // --- Path-containment guard against forged tokens (security hardening, MR !665 review) ---
  //
  // stringToFileData(String) performs NO validation and NO signature check (plain Base64+gzip,
  // forgeable offline by any authenticated user). Without a containment guard, a forged
  // fileName/relativePath/basePath escaping the upload area would be read from disk and, if it
  // happens to decode as an image, would become the caller's avatar and be served back via
  // GET /avatar — a scoped authenticated arbitrary-image-file READ oracle. The guard MUST run
  // BEFORE the validate-and-delete-orphan-on-failure block below: on a forged path, calling
  // fileService.deleteFile(stagedFileData) would DELETE THE ARBITRARY TARGET FILE, which is why
  // these tests assert fileService is never touched at all for a rejected forged token.
  //
  // Post-MR !665 review fix: the guard now delegates to fileService.isPathWithinUploadArea, a real
  // canonical-containment check, instead of comparing a raw path segment against
  // FileUtil.fixUntrustedPath's "fixed" form (which incorrectly rejected legitimate absolute
  // basePaths — fixUntrustedPath always prepends "." and re-normalizes, so a legit absolute path
  // never equals its own "fixed" form). These tests stub isPathWithinUploadArea(...)=false to
  // simulate a forged token being rejected by FileService's real containment logic.

  @Test
  void saveUserAvatarRejectsForgedFileNameEscapingUploadDirAndNeverDeletes() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(false);

    FileData forgedFileData = new FileData("..\\..\\..\\..\\somefile", 10L, "image/png", "avatar");
    String forgedToken = FileUtil.fileDataToString(forgedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(forgedToken));

    verifyNoInteractions(maintainService);
    verify(fileService, never()).deleteFile(any());
    verify(fileService, never()).getFullPath(any(), anyBoolean());
  }

  @Test
  void saveUserAvatarRejectsForgedRelativePathEscapingUploadDirAndNeverDeletes() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(false);

    FileData forgedFileData = new FileData("avatar.png", 10L, "image/png", "..\\..\\..\\..\\etc");
    String forgedToken = FileUtil.fileDataToString(forgedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(forgedToken));

    verifyNoInteractions(maintainService);
    verify(fileService, never()).deleteFile(any());
    verify(fileService, never()).getFullPath(any(), anyBoolean());
  }

  @Test
  void saveUserAvatarRejectsForgedAbsoluteBasePathEscapingUploadDirAndNeverDeletes() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(false);

    FileData forgedFileData = new FileData("avatar.png", 10L, "image/png", "avatar");
    forgedFileData.setBasePath("/etc/");
    String forgedToken = FileUtil.fileDataToString(forgedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(forgedToken));

    verifyNoInteractions(maintainService);
    verify(fileService, never()).deleteFile(any());
    verify(fileService, never()).getFullPath(any(), anyBoolean());
  }

  @Test
  void saveUserAvatarRejectsDisallowedMimeTypeAndDeletesOrphan() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);

    FileData stagedFileData = new FileData("malware.exe", 10L, "application/octet-stream", "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "malware.exe".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarRejectsOversizedFileAndDeletesOrphan() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);

    FileData stagedFileData = new FileData("avatar.png", 2L * 1024 * 1024 + 1, "image/png", "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "avatar.png".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarRejectsContentThatDoesNotDecodeAsImageAndDeletesOrphan() throws Exception {
    mockLocale();

    byte[] notAnImage = "<html><script>alert(1)</script></html>".getBytes();
    FileData stagedFileData = stageFile("avatar.png", "image/png", notAnImage);
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "avatar.png".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarResolvesTargetUserSolelyFromSession() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    FileData stagedFileData = stageFile("avatar.png", "image/png", realPngBytes());
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    userSettingsService.saveUserAvatar(stagedToken);

    // saveUserAvatar has no username parameter: the target user is resolved exclusively via
    // getSession().getUser() (called once to check for a previous token, once to upsert),
    // verified by mockCurrentUser("test") being the only user source.
    verify(aweSession, times(2)).getUser();
  }

  /**
   * WebP is intentionally excluded from the allow-list: stock JDK 17 {@link ImageIO} has no
   * built-in WebP reader (no TwelveMonkeys/WebP plugin on the classpath), so
   * {@code validateAvatarContentIsImage} would always decode {@code null} for a real WebP file
   * and reject every legitimate upload. This test uses real, well-formed WebP bytes (not garbage
   * bytes, which would be rejected for the unrelated reason of failing content decoding either
   * way) and asserts the claim is rejected at the MIME allow-list stage — the staged file's
   * content is never even read — proving the rejection reason is "WebP is not allowed", not "this
   * WebP failed to decode".
   */
  @Test
  void saveUserAvatarRejectsWebpMimeTypeBeforeAttemptingContentValidationAndDeletesOrphan() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);

    FileData stagedFileData = new FileData("avatar.webp", (long) realWebpBytes().length, "image/webp", "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "avatar.webp".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarRejectsEmptyFileAndDeletesOrphan() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);

    // Empty file: no declared MIME type resolvable (mirrors the retired MultipartFile behavior
    // of an unset/blank content type on a zero-byte file), rejected at the MIME allow-list stage.
    FileData stagedFileData = new FileData("avatar.png", 0L, null, "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "avatar.png".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarRejectsMissingMimeTypeAndDeletesOrphan() throws Exception {
    mockLocale();
    when(fileService.isPathWithinUploadArea(any(FileData.class))).thenReturn(true);

    FileData stagedFileData = new FileData("avatar.png", 10L, null, "avatar");
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    assertThrows(AWException.class, () -> userSettingsService.saveUserAvatar(stagedToken));

    verify(fileService, times(1)).deleteFile(argThat(fd -> "avatar.png".equals(fd.getFileName())));
    verifyNoInteractions(maintainService);
  }

  @Test
  void saveUserAvatarDeletesPreviousFileAfterSuccessfulClaim() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

    String oldToken = FileUtil.fileDataToString(new FileData("old.png", 512L, "image/png"));
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataListWithToken(oldToken)));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    when(fileService.deleteFile(any(FileData.class))).thenReturn(new ServiceData());

    FileData stagedFileData = stageFile("new.png", "image/png", realPngBytes());
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    userSettingsService.saveUserAvatar(stagedToken);

    verify(fileService, times(1)).deleteFile(any(FileData.class));
  }

  @Test
  void saveUserAvatarContinuesWhenOldFileDeleteFails() throws Exception {
    mockCurrentUser("test");
    when(queryUtil.getParameters(any(), anyString(), anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

    String oldToken = FileUtil.fileDataToString(new FileData("old.png", 512L, "image/png"));
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataListWithToken(oldToken)));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    when(fileService.deleteFile(any(FileData.class))).thenThrow(new AWException("Delete failed", "Delete failed"));

    FileData stagedFileData = stageFile("new.png", "image/png", realPngBytes());
    String stagedToken = FileUtil.fileDataToString(stagedFileData);

    // Must not propagate the delete failure: the claim itself already succeeded
    assertDoesNotThrow(() -> userSettingsService.saveUserAvatar(stagedToken));
  }
}
