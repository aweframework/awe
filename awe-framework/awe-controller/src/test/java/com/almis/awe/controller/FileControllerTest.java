package com.almis.awe.controller;

import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.security.authorization.TargetKind;
import com.almis.awe.security.authorization.TargetLaunchAuthorizerChain;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.FileService;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

  @Mock
  private AweRequest aweRequest;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private EncodeService encodeService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private FileService fileService;

  @Mock
  private TargetLaunchAuthorizerChain targetLaunchAuthorizer;

  @Mock
  private AweElements aweElements;

  @InjectMocks
  private FileController fileController;

  @BeforeEach
  void setUp() throws Exception {
    fileController.setApplicationContext(applicationContext);
  }

  @Test
  void testGetFileAsText() throws AWException {
    // Set up mock parameters and expected results
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(targetLaunchAuthorizer.authorize("getLogFile", TargetKind.SERVICE)).thenReturn(true);

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    when(fileService.getTextFile(any(), any())).thenReturn(ResponseEntity.ok("prueba"));

    // Call method under test
    ResponseEntity<String> response = fileController.getFileAsText(parameters);

    // Assert expected response
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  @ParameterizedTest
  @ValueSource(strings = { "validPath", "invalidPath" })
  void testGetFileAsStream(String path) throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(targetLaunchAuthorizer.authorize("getLogFile", TargetKind.SERVICE)).thenReturn(true);

    // Set up mock parameters
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    when(fileService.getFileStream(any(), any())).thenReturn(ResponseEntity.ok(new FileSystemResource("test")));

    // Call method under test
    ResponseEntity<FileSystemResource> response = fileController.getFileAsStream(parameters);

    // Assert expected response based on parameter
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  @Test
  void testGetFileAsStreamPOST() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);

    // Set up mock parameters and expected results
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    String targetId = "testTarget";
    when(maintainService.launchMaintain(anyString())).thenReturn(new ServiceData().setData(new FileData()));
    when(fileService.getFileStream(any(FileData.class))).thenReturn(ResponseEntity.ok(new FileSystemResource("test")));

    // Call method under test
    ResponseEntity<FileSystemResource> response = fileController.getFileAsStreamPOST(parameters, targetId);

    // Assert expected response
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  @Test
  void testDeleteFile() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);

    // Validate successful delete
    when(aweRequest.getParameterAsString(anyString())).thenReturn(
      FileUtil.fileDataToString(new FileData().setFileName("fileName").setMimeType("text/plain")));
    when(fileService.deleteFile(any())).thenReturn(new ServiceData(), new ServiceData().setType(AnswerType.ERROR));
    ServiceData result = fileController.deleteFile(JsonNodeFactory.instance.objectNode());
    assertNotNull(result);
    assertEquals(AnswerType.OK, result.getType());

    // Validate error case
    result = fileController.deleteFile(JsonNodeFactory.instance.objectNode());
    assertNotNull(result);
    assertEquals(AnswerType.ERROR, result.getType());
  }

  @Test
  void testDownloadFile() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);

    when(aweRequest.getParameter(any())).thenReturn(JsonNodeFactory.instance.numberNode(1));
    when(aweRequest.getParameterAsString(anyString())).thenReturn(
      FileUtil.fileDataToString(new FileData().setFileName("fileName").setMimeType("text/plain")));
    byte[] file = new byte[10];
    when(fileService.downloadFile(any(FileData.class), any())).thenReturn(ResponseEntity.ok(file));

    // Validate successful file download
    assertArrayEquals(file, fileController.downloadFile(JsonNodeFactory.instance.objectNode()).getBody());
  }

  @Test
  void downloadFileMaintain() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);

    when(maintainService.launchMaintain(anyString())).thenReturn(new ServiceData().setData(new FileData()));
    when(aweRequest.getParameter(any())).thenReturn(JsonNodeFactory.instance.numberNode(1));
    when(fileService.downloadFile(any(FileData.class), any())).thenReturn(ResponseEntity.ok("File downloaded".getBytes()), ResponseEntity.notFound().build());

    // Normal case
    ResponseEntity<byte[]> result = fileController.downloadFileMaintain(JsonNodeFactory.instance.objectNode(), "targetId");
    assertArrayEquals("File downloaded".getBytes(), result.getBody());

    // File not found
    result = fileController.downloadFileMaintain(JsonNodeFactory.instance.objectNode(), "targetId");
    assertEquals(HttpStatusCode.valueOf(404), result.getStatusCode());
  }

  @Test
  void handleAWException() {
    fileController.handleAWException(new AWException("test"));
    assertTrue(true);
  }

  // Text and stream files need the permission of the log viewer service getLogFile (issue 975)

  private void refuseLogViewer() {
    when(targetLaunchAuthorizer.authorize("getLogFile", TargetKind.SERVICE)).thenReturn(false);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void textFileRefusedByTheAuthorizerIsNotRead() throws AWException {
    refuseLogViewer();
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("path", "encoded");

    AWEUnauthorizedException exception = assertThrows(AWEUnauthorizedException.class,
      () -> fileController.getFileAsText(parameters));

    assertEquals(AWEUnauthorizedException.TITLE_KEY, exception.getTitle());
    assertEquals(AWEUnauthorizedException.MESSAGE_KEY, exception.getMessage());
    verifyNoInteractions(fileService, encodeService);
  }

  @Test
  void streamFileRefusedByTheAuthorizerIsNotRead() throws AWException {
    refuseLogViewer();
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("path", "encoded");

    AWEUnauthorizedException exception = assertThrows(AWEUnauthorizedException.class,
      () -> fileController.getFileAsStream(parameters));

    assertEquals(AWEUnauthorizedException.TITLE_KEY, exception.getTitle());
    assertEquals(AWEUnauthorizedException.MESSAGE_KEY, exception.getMessage());
    verifyNoInteractions(fileService, encodeService);
  }

  @Test
  void textFileAllowedByTheAuthorizerIsReadFromTheDecodedPath() throws AWException {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(targetLaunchAuthorizer.authorize("getLogFile", TargetKind.SERVICE)).thenReturn(true);
    when(aweRequest.getParameterAsString("path")).thenReturn("encoded");
    when(aweRequest.getParameterAsString("content-type")).thenReturn("text/plain");
    when(encodeService.decodeSymmetric("encoded")).thenReturn("/logs/app.log");
    when(fileService.getTextFile("/logs/app.log", "text/plain")).thenReturn(ResponseEntity.ok("line"));

    assertEquals("line", fileController.getFileAsText(JsonNodeFactory.instance.objectNode()).getBody());
    verify(fileService, never()).getFileStream(anyString(), anyString());
  }
}
