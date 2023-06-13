package com.almis.awe.test.integration.controller;

import com.almis.awe.controller.TemplateController;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.almis.awe.test.integration.util.TestUtil.readFileAsText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pgarcia
 */
@Tag("integration")
@DisplayName("Template controller Tests")
@Slf4j
@WithAnonymousUser
class TemplateControllerTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  TemplateController templateController;
  @Autowired
  AweSession aweSession;

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

    assertEquals(expected, result.getResponse().getContentAsString().replaceAll("\\n|\\r\\n", System.getProperty("line.separator")));
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(templateController);
  }

  /**
   * Test of getAngularTemplate method, of class TemplateController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetAngularTemplate() throws Exception {
    testTemplate("templates/Modal.txt", "/template/angular/confirm", status().isOk());
  }

  /**
   * Test of getAngularSubTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  void testGetAngularSubTemplate() throws Exception {
    testTemplate("templates/Criterion.txt", "/template/angular/input/checkbox", status().isOk());
  }

  /**
   * Test of getScreenTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  void testGetScreenTemplate() throws Exception {
    testTemplate("templates/Screen.txt", "/template/screen/base/information", status().isOk());
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  void testGetErrorScreenTemplate() throws Exception {
    testTemplate("templates/ScreenError.txt", "/template/screen/base/pantalla-inexistente", status().isUnauthorized());
  }

  /**
   * Test of getDefaultScreenTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  void testGetDefaultScreenTemplate() throws Exception {
    testTemplate("templates/DefaultScreen.txt", "/template/screen/", status().isOk());
  }

  /**
   * Test of public screen
   *
   * @throws Exception test error
   */
  @Test
  void testGetPublicScreenTemplate() throws Exception {
    testTemplate("templates/DefaultScreen.txt", "/template/screen/base/signin", status().isOk());
  }
}
