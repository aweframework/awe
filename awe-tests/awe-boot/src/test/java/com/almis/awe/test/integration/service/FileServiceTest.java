package com.almis.awe.test.integration.service;

import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.FileService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author pgarcia
 */
@Tag("integration")
@DisplayName("File service Tests")
class FileServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private FileService fileService;

  /**
   * Test of upload, download and delete file
   *
   * @throws Exception Test error
   */
  @Test
  void testUploadDownloadAndDeleteFile() throws Exception {
    MultipartFile file = new MockMultipartFile("test", "test", "text/plain", "dummy".getBytes());
    FileData fileData = fileService.uploadFile(file, null);
    assertEquals(HttpStatus.OK, fileService.downloadFile(fileData, 0).getStatusCode());
    assertEquals(AnswerType.OK, fileService.deleteFile(fileData).getType());
  }

  /**
   * Test of upload, download and delete file
   * @throws Exception Test error
   */
  @Test
  void testDownloadMockedInputStream() throws Exception {
    String input = "some test data for my input stream";
    InputStream inputStream = IOUtils.toInputStream(input, "UTF-8");
    FileData fileData = new FileData("tutu", (long) input.getBytes().length, "application/pdf");
    fileData.setFileStream(inputStream);
    ResponseEntity<byte[]> fileDownloaded = fileService.downloadFile(fileData, 0);
    assertEquals(HttpStatus.OK, fileDownloaded.getStatusCode());
    assertEquals("tutu", Objects.requireNonNull(fileDownloaded.getHeaders().get("Filename")).get(0));
    assertEquals(MediaType.APPLICATION_PDF, fileDownloaded.getHeaders().getContentType());
  }
}