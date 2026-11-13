package com.almis.awe.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves a fixed text to the file-viewer test screen.
 * The file-viewer widget posts to /{server-action}/{target-action} and shows the body only when the status is 201, which
 * is what /file/text answers, but that endpoint needs the encoded path of a file on the server. The screen uses
 * server-action="file" (the only generic file action of the screen schema) with the target-action "test-text".
 */
@RestController
public class TestFileController {

  /**
   * Retrieve the text of the test file
   *
   * @return Text with the created status
   */
  @PostMapping("/file/test-text")
  public ResponseEntity<String> getText() {
    return ResponseEntity.status(HttpStatus.CREATED)
      .contentType(MediaType.TEXT_PLAIN)
      .body("FILE VIEWER TEST LINE 1\nFILE VIEWER TEST LINE 2\nFILE VIEWER TEST LINE 3\n");
  }
}
