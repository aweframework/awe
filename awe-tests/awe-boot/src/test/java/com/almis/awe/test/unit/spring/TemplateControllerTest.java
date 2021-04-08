package com.almis.awe.test.unit.spring;

import com.almis.awe.controller.ActionController;
import com.almis.awe.controller.SettingsController;
import com.almis.awe.controller.TemplateController;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import javax.naming.NamingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pgarcia
 */
@Log4j2
@WithAnonymousUser
public class TemplateControllerTest extends AweSpringBootTests {

  private ActionController actionController;
  private SettingsController settingsController;
  private TemplateController templateController;

  @Before
  public void initBeans() {
    actionController = getBean(ActionController.class);
    settingsController = getBean(SettingsController.class);
    templateController = getBean(TemplateController.class);
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
    MvcResult result = mockMvc.perform(get(templatePath).accept("text/html;charset=UTF-8"))
      .andExpect(statusResultMatcher)
      .andExpect(content().encoding("UTF-8"))
      .andReturn();

    assertEquals(result.getResponse().getContentAsString().replaceAll("\\n|\\r\\n", System.getProperty("line.separator")), expected);
  }

  /**
   * Test context loaded
   *
   * @throws NamingException Test error
   */
  @Test
  public void contextLoads() throws NamingException {
    // Check that controller are active
    assertThat(actionController).isNotNull();
    assertThat(settingsController).isNotNull();
    assertThat(templateController).isNotNull();
  }

  /**
   * Test of getAngularTemplate method, of class TemplateController.
   *
   * @throws Exception Test error
   */
  @Test
  public void testGetAngularTemplate() throws Exception {
    testTemplate("templates/Modal.txt", "/template/angular/confirm", status().isOk());
  }

  /**
   * Test of getAngularSubTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  public void testGetAngularSubTemplate() throws Exception {
    testTemplate("templates/Criterion.txt", "/template/angular/input/text", status().isOk());
  }

  /**
   * Test of getScreenTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  public void testGetScreenTemplate() throws Exception {
    testTemplate("templates/Screen.txt", "/template/screen/base/information", status().isOk());
  }

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @throws Exception Test error
   */
  @Test
  public void testGetErrorScreenTemplate() throws Exception {
    testTemplate("templates/ScreenError.txt", "/template/screen/base/pantalla-inexistente", status().isUnauthorized());
  }

  /**
   * Test of getDefaultScreenTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  public void testGetDefaultScreenTemplate() throws Exception {
    testTemplate("templates/DefaultScreen.txt", "/template/screen/", status().isOk());
  }

  /**
   * Test of public screen
   *
   * @throws Exception test error
   */
  @Test
  public void testGetPublicScreenTemplate() throws Exception {
    testTemplate("templates/DefaultScreen.txt", "/template/screen/base/signin", status().isOk());
  }

  /**
   * Test of getAngularTemplate method, of class TemplateController.
   *
   * @throws Exception Test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  public void testGetSitesHelpTemplate() throws Exception {
    given(aweSession.isAuthenticated()).willReturn(true);
    testTemplate("context-help/ScreenHelp.txt", "/template/help/sites", status().isOk());
  }

  /**
   * Test of getAngularSubTemplate method, of class TemplateController.
   *
   * @throws Exception test error
   */
  @Test
  @WithMockUser(username = "test", password = "test")
  public void testGetApplicationHelpTemplate() throws Exception {
    given(aweSession.isAuthenticated()).willReturn(true);
    testTemplate("context-help/ApplicationHelp.txt", "/template/help", status().isOk());
  }

  /**
   * Test of getSettings method, of class SettingsController.
   *
   * @throws Exception Test error
   */
  @Test
  public void testGetSettings() throws Exception {
    String expected = "{\"pathServer\":\"\",\"initialURL\":\"\",\"language\":\"en\",\"theme\":\"sky\",\"charset\":\"UTF-8\",\"applicationName\":\"awe-boot\",\"dataSuffix\":\".data\",\"homeScreen\":\"screen/home\",\"recordsPerPage\":30,\"recordsPerPageOnCriteria\":100,\"pixelsPerCharacter\":8,\"defaultComponentSize\":\"sm\",\"reloadCurrentScreen\":false,\"suggestTimeout\":300,\"connectionProtocol\":\"COMET\",\"connectionTransport\":\"websocket\",\"connectionBackup\":\"streaming\",\"connectionTimeout\":60000000,\"uploadIdentifier\":\"u\",\"downloadIdentifier\":\"d\",\"uploadMaxSize\":500,\"addressIdentifier\":\"address\",\"passwordPattern\":\".*\",\"minlengthPassword\":4,\"encodeTransmission\":false,\"encodeKey\":\"p\",\"tokenKey\":\"t\",\"actionsStack\":0,\"debug\":\"INFO\",\"loadingTimeout\":10000,\"helpTimeout\":1000,\"messageTimeout\":{\"info\":0,\"error\":0,\"validate\":2000,\"help\":5000,\"warning\":4000,\"ok\":2000,\"wrong\":0,\"chat\":0},\"numericOptions\":{\"pSign\":\"s\",\"aDec\":\",\",\"vMin\":-1.0E10,\"dGroup\":3,\"vMax\":1.0E10,\"mDec\":5,\"mRound\":\"S\",\"aPad\":false,\"wEmpty\":\"empty\",\"aSep\":\".\",\"aSign\":\"\"},\"pivotOptions\":{\"numGroup\":5000},\"chartOptions\":{\"limitPointsSerie\":1000000}}";
    ObjectNode expectedJson = (ObjectNode) objectMapper.readTree(expected);
    MvcResult mvcResult = mockMvc.perform(post("/settings")
      .header("Authorization", "b0d28a33-eea9-44c6-a142-a7fc6bfb7afa")
      .content("{\"view\":\"base\"}")
      .accept("application/json")).andExpect(status().isOk())
      .andExpect(content().json(expected))
      .andReturn();

    String result = mvcResult.getResponse().getContentAsString();
    ObjectNode retrievedJson = (ObjectNode) objectMapper.readTree(result);

    // Check objects
    assertEquals(expectedJson, retrievedJson);
  }
}
