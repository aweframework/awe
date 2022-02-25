package com.almis.awe.test.integration.controller;

import com.almis.awe.factory.WithMockCustomUser;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Upload controller tests
 *
 * @author pgarcia
 */
@Tag("integration")
@DisplayName("Upload controller Tests")
@WithMockCustomUser(username = "test", password = "test")
class UploadControllerTest extends AbstractSpringAppIntegrationTest {

  // Constants
  private static final String TOKEN = "16617f0d-97ee-4f6b-ad54-905d6ce3c328";

  // Upload identifier
  @Value("${file.upload.identifier}")
  private String uploadIdentifierKey;

  /**
   * Test a UPLOAD POST
   *
   * @param file File to upload
   * @throws Exception exception
   */
  private void doUploadTest(MockMultipartFile file, ResultMatcher status) throws Exception {
    mockMvc.perform(multipart("/file/upload")
            .file(file)
            .with(csrf())
            .param("address", "{}")
            .param("destination", "")
            .param(uploadIdentifierKey, "uploader-test-1")
            .header("Authorization", TOKEN))
            .andExpect(status)
            .andReturn();
  }

  /**
   * Test upload file ok
   *
   * @throws Exception Test error
   */
  @Test
  void testUploadOK() throws Exception {
    String fileName = "orig";
    String content = "bar";
    MockMultipartFile file = new MockMultipartFile("file", fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, content.getBytes());
    doUploadTest(file, status().isOk());
  }

  /**
   * Test upload file ko
   *
   * @throws Exception Test error
   */
  @Test
  void testUploadKO() throws Exception {
    String fileName = "orig";
    String content = "bar";
    MockMultipartFile file = new MockMultipartFile("kk", fileName, MediaType.APPLICATION_JSON_VALUE, content.getBytes());
    doUploadTest(file, status().is4xxClientError());
  }
}