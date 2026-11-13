package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.FileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link FileService#isPathWithinUploadArea(FileData)} (post-MR !665 review fix,
 * Slice 6 hardening). The guard is a platform-independent string check: {@code basePath} must equal
 * the configured upload root exactly, and {@code relativePath}/{@code fileName} must not contain
 * {@code ..} traversal or be absolute. Fixtures avoid OS-specific separators so the assertions hold
 * identically on Windows and Linux (an earlier canonical-path implementation passed on Windows but
 * mis-accepted backslash "traversal" on Linux, where {@code \\} is a literal filename character).
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final String UPLOAD_ROOT = "@upload/";

  @InjectMocks
  private FileService fileService;

  @Mock
  private BroadcastService broadcastService;

  @Mock
  private AweRequest request;

  @Mock
  private BaseConfigProperties baseConfigProperties;

  @Mock
  private EncodeService encodeService;

  @TempDir
  Path tempDir;

  private BaseConfigProperties.Component component;
  private final List<String> transfers = new ArrayList<>();

  @BeforeEach
  void setUp() {
    component = new BaseConfigProperties.Component();
    component.setUploadFilePath(UPLOAD_ROOT);
    lenient().when(baseConfigProperties.getComponent()).thenReturn(component);
    lenient().when(baseConfigProperties.getPaths()).thenReturn(new BaseConfigProperties.Paths());
  }

  private void observeTransfers() {
    fileService.setFileTransferObserver((direction, bytes) -> transfers.add(direction + ":" + bytes));
  }

  private FileData staged(String fileName, String relativePath, String basePath) {
    FileData fileData = new FileData(fileName, 10L, "image/png", relativePath);
    fileData.setBasePath(basePath);
    return fileData;
  }

  @Test
  void legitStagedFileIsWithinUploadArea() {
    assertTrue(fileService.isPathWithinUploadArea(staged("avatar.png", "avatar", UPLOAD_ROOT)));
  }

  @Test
  void legitStagedFileWithNoRelativePathIsWithinUploadArea() {
    assertTrue(fileService.isPathWithinUploadArea(staged("avatar.png", null, UPLOAD_ROOT)));
  }

  @Test
  void forgedBasePathOutsideUploadRootIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("avatar.png", "avatar", "/etc/")));
  }

  @Test
  void nullBasePathIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("avatar.png", "avatar", null)));
  }

  @Test
  void forgedFileNameWithForwardSlashTraversalIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("../../../etc/passwd", "avatar", UPLOAD_ROOT)));
  }

  @Test
  void forgedFileNameWithBackslashTraversalIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("..\\..\\..\\secret", "avatar", UPLOAD_ROOT)));
  }

  @Test
  void forgedRelativePathWithTraversalIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("avatar.png", "../../../etc", UPLOAD_ROOT)));
  }

  @Test
  void forgedAbsoluteRelativePathIsRejected() {
    assertFalse(fileService.isPathWithinUploadArea(staged("avatar.png", "/etc", UPLOAD_ROOT)));
  }

  @Test
  void uploadedFileIsObservedWithItsSize() throws Exception {
    component.setUploadFilePath(tempDir.toString() + "/");
    observeTransfers();

    FileData fileData = fileService.uploadFile(new MockMultipartFile("file", "data.txt", "text/plain", new byte[]{1, 2, 3, 4, 5}), "folder");

    assertEquals(5L, fileData.getFileSize());
    assertEquals(List.of("UPLOAD:5"), transfers);
  }

  @Test
  void emptyUploadIsNotObserved() throws Exception {
    component.setUploadFilePath(tempDir.toString() + "/");
    observeTransfers();

    fileService.uploadFile(new MockMultipartFile("file", "data.txt", "text/plain", new byte[0]), "folder");

    assertTrue(transfers.isEmpty());
  }

  @Test
  void downloadedFileIsObservedWithTheBytesSent() throws Exception {
    observeTransfers();
    FileData fileData = staged("data.txt", "folder", tempDir.toString() + "/");
    fileData.setMimeType("text/plain");
    fileData.setFileStream(new ByteArrayInputStream(new byte[]{1, 2, 3}));

    assertEquals(3, fileService.downloadFile(fileData, 1).getBody().length);
    assertEquals(List.of("DOWNLOAD:3"), transfers);
  }

  @Test
  void transfersWithoutObserverRunAsBefore() throws Exception {
    component.setUploadFilePath(tempDir.toString() + "/");

    assertEquals(2L, fileService.uploadFile(new MockMultipartFile("file", "data.txt", "text/plain", new byte[]{1, 2}), "folder").getFileSize());
  }

  // Log viewer (get-log-file): only the files under the log directories can be read (issue 975)

  private Path logDirectory() throws Exception {
    Path logDirectory = Files.createDirectories(tempDir.resolve("logs"));
    lenient().when(baseConfigProperties.getLogManagerPath()).thenReturn(logDirectory.toString());
    return logDirectory;
  }

  @Test
  void logFileUnderTheLogDirectoryIsAllowed() throws Exception {
    Path logDirectory = logDirectory();
    Path logFile = Files.writeString(Files.createDirectories(logDirectory.resolve("user")).resolve("app.log"), "line");

    assertTrue(fileService.isWithinLogDirectories(logFile.toString()));
    assertTrue(fileService.isWithinLogDirectories(logDirectory.resolve("not-created-yet.log").toString()));
  }

  @Test
  void fileOutsideTheLogDirectoryIsRejected() throws Exception {
    logDirectory();
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret");

    assertFalse(fileService.isWithinLogDirectories(secret.toString()));
    assertFalse(fileService.isWithinLogDirectories(tempDir.resolve("logs-other").resolve("app.log").toString()));
  }

  @Test
  void traversalOutOfTheLogDirectoryIsRejected() throws Exception {
    Path logDirectory = logDirectory();
    Files.writeString(tempDir.resolve("secret.txt"), "secret");

    assertFalse(fileService.isWithinLogDirectories(logDirectory + "/../secret.txt"));
    assertFalse(fileService.isWithinLogDirectories("../../../../../../etc/passwd"));
  }

  @Test
  void symbolicLinkOutOfTheLogDirectoryIsRejected() throws Exception {
    Path logDirectory = logDirectory();
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret");
    Path link = logDirectory.resolve("link.log");
    try {
      Files.createSymbolicLink(link, secret);
    } catch (UnsupportedOperationException | java.io.IOException exc) {
      assumeTrue(false, "Symbolic links not supported: " + exc.getMessage());
    }

    assertFalse(fileService.isWithinLogDirectories(link.toString()));
  }

  @Test
  void blankPathOrTheLogDirectoryItselfIsRejected() throws Exception {
    Path logDirectory = logDirectory();

    assertFalse(fileService.isWithinLogDirectories(null));
    assertFalse(fileService.isWithinLogDirectories(""));
    assertFalse(fileService.isWithinLogDirectories(logDirectory.toString()));
  }

  @Test
  void logFileUnderAProvidedLogDirectoryIsAllowed() throws Exception {
    logDirectory();
    Path executionLogs = Files.createDirectories(tempDir.resolve("scheduler"));
    Path executionLog = Files.writeString(executionLogs.resolve("execution_1-1.log"), "line");
    assertFalse(fileService.isWithinLogDirectories(executionLog.toString()));

    fileService.setLogDirectoryProviders(List.of(() -> executionLogs));

    assertTrue(fileService.isWithinLogDirectories(executionLog.toString()));
  }

  @Test
  void fileSystemRootIsNeverALogDirectory() throws Exception {
    Path secret = Files.writeString(tempDir.resolve("secret.txt"), "secret");
    lenient().when(baseConfigProperties.getLogManagerPath()).thenReturn(secret.getRoot().toString());

    assertFalse(fileService.isWithinLogDirectories(secret.toString()));
  }

  // Text and stream files (/file/text, /file/stream): only the files under the log directories can be read (issue 975)

  /**
   * Locales resolved as their key followed by their parameters, so a message that echoes the path shows it
   */
  private void localesAsKeys() {
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    lenient().when(context.getBean(AweElements.class)).thenReturn(elements);
    lenient().when(elements.getLocaleWithLanguage(anyString(), any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(elements.getLocaleWithLanguage(anyString(), any(), any(Object[].class)))
      .thenAnswer(invocation -> invocation.getArgument(0) + " " + Arrays.toString(
        Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length)));
    fileService.setApplicationContext(context);
  }

  private void assertGenericReadingError(AWException exception, String path) {
    assertEquals("ERROR_TITLE_FILE_READING_ERROR", exception.getTitle());
    assertEquals("ERROR_MESSAGE_FILE_READING_ERROR", exception.getMessage());
    assertFalse(exception.getMessage().contains(path));
  }

  @Test
  void textFileUnderTheLogDirectoryIsRead() throws Exception {
    Path logFile = Files.writeString(logDirectory().resolve("app.log"), "line");

    ResponseEntity<String> response = fileService.getTextFile(logFile.toString(), "text/plain");

    assertEquals("line", response.getBody());
  }

  @Test
  void textFileOutsideTheLogDirectoryIsRejectedWithoutThePath() throws Exception {
    localesAsKeys();
    Path logDirectory = logDirectory();
    Files.writeString(tempDir.resolve("secret.txt"), "secret");
    String path = logDirectory + "/../secret.txt";

    assertGenericReadingError(assertThrows(AWException.class, () -> fileService.getTextFile(path, "text/plain")), path);
  }

  @Test
  void missingTextFileUnderTheLogDirectoryFailsWithoutThePath() throws Exception {
    localesAsKeys();
    String path = logDirectory().resolve("missing.log").toString();

    assertGenericReadingError(assertThrows(AWException.class, () -> fileService.getTextFile(path, "text/plain")), path);
  }

  @Test
  void streamFileUnderTheLogDirectoryIsRead() throws Exception {
    Path logFile = Files.writeString(logDirectory().resolve("app.log"), "line");

    ResponseEntity<FileSystemResource> response = fileService.getFileStream(logFile.toString(), "text/plain");

    assertEquals(4L, response.getHeaders().getContentLength());
    assertEquals(logFile.toFile(), response.getBody().getFile());
  }

  @Test
  void streamFileOutsideTheLogDirectoryIsRejectedWithoutThePath() throws Exception {
    localesAsKeys();
    logDirectory();
    String path = Files.writeString(tempDir.resolve("secret.txt"), "secret").toString();

    assertGenericReadingError(assertThrows(AWException.class, () -> fileService.getFileStream(path, "text/plain")), path);
  }

  @Test
  void missingStreamFileUnderTheLogDirectoryFailsWithoutThePath() throws Exception {
    localesAsKeys();
    String path = logDirectory().resolve("missing.log").toString();

    assertGenericReadingError(assertThrows(AWException.class, () -> fileService.getFileStream(path, "text/plain")), path);
  }

  @Test
  void streamOfAnUploadedFileOutsideTheLogDirectoriesIsStillRead() throws Exception {
    logDirectory();
    Files.writeString(Files.createDirectories(tempDir.resolve("upload").resolve("avatar")).resolve("me.png"), "png");
    FileData fileData = staged("me.png", "avatar", tempDir.resolve("upload") + "/");

    assertEquals(3L, fileService.getFileStream(fileData).getHeaders().getContentLength());
  }
}
