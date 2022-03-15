package com.almis.awe.test.integration.controller;

import com.almis.awe.factory.WithMockCustomUser;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.almis.awe.test.integration.util.TestUtil.readFileAsText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 *
 */
@Tag("integration")
@DisplayName("Help controller Tests")
class HelpControllerTest extends AbstractSpringAppIntegrationTest {

  /**
   * Test of getAngularTemplate method, of class TemplateController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithMockCustomUser(username = "test", password = "test")
  void testGetSitesHelpTemplate() throws Exception {
    testTemplate("context-help/ScreenHelp.txt", "/template/help/sites", status().isOk());
  }

  /**
   * Test of getAngularSubTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  @WithMockCustomUser(username = "test", password = "test")
  void testGetApplicationHelpTemplate() throws Exception {
    testTemplate("context-help/ApplicationHelp.txt", "/template/help", status().isOk());
  }

  /**
   * Test template file
   *
   * @param filePath            File to test
   * @param templatePath        Template path
   * @param statusResultMatcher Result matcher
   * @throws Exception Error in template retrieval
   */
  private void testTemplate(String filePath, String templatePath, ResultMatcher statusResultMatcher) throws Exception {
    String expected = readFileAsText(filePath).replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
    MvcResult result = mockMvc.perform(get(templatePath)
        .accept("text/html;charset=UTF-8")
        .with(csrf()))
      .andExpect(statusResultMatcher)
      .andExpect(content().encoding("UTF-8"))
      .andReturn();

    assertEquals(result.getResponse().getContentAsString().replaceAll("\\n|\\r\\n", System.getProperty("line.separator")), expected);
  }
}
