package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static com.almis.awe.test.integration.util.TestUtil.readFileAsText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
  @WithMockUser(username = "test", password = "test")
  void testGetSitesHelpTemplate() throws Exception {
    testTemplate("context-help/ScreenHelp.txt", "/template/help/sites", status().isOk());
  }

  /**
   * Test of getAngularSubTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetApplicationHelpTemplate() throws Exception {
    testTemplate("context-help/ApplicationHelp.txt", "/template/help", status().isOk());
  }

  /**
   * The help of the mail servers option is the help of the email-server-admin screen it opens, not the help of the EmlSrv
   * screen it replaces
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetEmailServersHelpTemplate() throws Exception {
    testHelpOfAdminScreen("email-servers", "SCREEN_TITLE_EML_SRV_ADMIN", "SCREEN_TITLE_EML_SRV");
  }

  /**
   * The help of the sequences option is the help of the sequence-admin screen it opens, not the help of the AweKey screen it
   * replaces. It is not compared with a golden file of the whole template: that file would change with every edit of the
   * screen, and what this test protects is which screen the option opens.
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetSequencesHelpTemplate() throws Exception {
    testHelpOfAdminScreen("sequences", "SCREEN_TITLE_SEQUENCE_ADMIN", "SCREEN_TITLE_AWEKEY");
  }

  /**
   * The help of the application parameters option is the help of the application-parameter-admin screen it opens, not the
   * help of the AweAppPar screen it replaces
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetApplicationParametersHelpTemplate() throws Exception {
    testHelpOfAdminScreen("application-parameters", "SCREEN_TITLE_APP_PAR_ADMIN", "SCREEN_TITLE_APP_PARAMETERS");
  }

  /**
   * The help of the modules option is the help of the module-admin screen it opens, not the help of the Mod screen it
   * replaces
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetModulesHelpTemplate() throws Exception {
    testHelpOfAdminScreen("modules", "SCREEN_TITLE_MOD_ADMIN", "SCREEN_TITLE_MDL");
  }

  /**
   * The help of the message queues option is the help of the queue-admin screen it opens, not the help of the Que screen it
   * replaces
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  void testGetQueuesHelpTemplate() throws Exception {
    testHelpOfAdminScreen("queues", "SCREEN_TITLE_QUEUE_ADMIN", "SCREEN_TITLE_QUE");
  }

  /**
   * Check that the help of a menu option is the help of the administration screen that the option opens
   *
   * @param option              Menu option
   * @param screenTitle         Title of the screen the option opens
   * @param previousScreenTitle Title of the screen the option opened before the redesign
   * @throws Exception Error in template retrieval
   */
  private void testHelpOfAdminScreen(String option, String screenTitle, String previousScreenTitle) throws Exception {
    mockMvc.perform(get("/template/help/" + option)
        .accept("text/html;charset=UTF-8")
        .with(csrf()))
      .andExpect(status().isOk())
      .andExpect(content().encoding("UTF-8"))
      .andExpect(content().string(containsString("id=\"" + screenTitle + "\"")))
      .andExpect(content().string(containsString("HELP_" + screenTitle)))
      .andExpect(content().string(not(containsString("id=\"" + previousScreenTitle + "\""))));
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

    assertEquals(expected, result.getResponse().getContentAsString().replaceAll("\\n|\\r\\n", System.getProperty("line.separator")));
  }
}
