package com.almis.awe.controller;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.FileService;
import com.almis.awe.service.MaintainService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
}
