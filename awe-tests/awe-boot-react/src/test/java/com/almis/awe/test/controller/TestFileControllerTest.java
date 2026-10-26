package com.almis.awe.test.controller;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Text served to the file-viewer test screen: it is fixed, so the browser tests always read the same lines
 */
// The surefire run of the test applications selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class TestFileControllerTest {

  private final TestFileController controller = new TestFileController();

  @Test
  void shouldAnswerTheFixedTextWithTheCreatedStatusThatTheFileViewerExpects() {
    ResponseEntity<String> response = controller.getText();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).contains("FILE VIEWER TEST LINE 1", "FILE VIEWER TEST LINE 3");
  }

  @Test
  void shouldServeThePostOfTheFileViewerAsPlainText() throws Exception {
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(post("/file/test-text").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isCreated())
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
      .andExpect(content().string(org.hamcrest.Matchers.containsString("FILE VIEWER TEST LINE 2")));
  }
}
