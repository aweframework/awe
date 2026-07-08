package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.FileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

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

  @BeforeEach
  void setUp() {
    BaseConfigProperties.Component component = new BaseConfigProperties.Component();
    component.setUploadFilePath(UPLOAD_ROOT);
    lenient().when(baseConfigProperties.getComponent()).thenReturn(component);
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
}
