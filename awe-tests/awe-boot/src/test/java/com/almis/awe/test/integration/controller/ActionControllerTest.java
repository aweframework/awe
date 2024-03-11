package com.almis.awe.test.integration.controller;

import com.almis.awe.model.details.MaintainResultDetails;
import com.almis.awe.model.type.MaintainType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import com.almis.awe.test.integration.util.TestUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.almis.awe.test.integration.util.TestUtil.assertResultJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for testing /action controller
 */
@Slf4j
@WithAnonymousUser
@DisplayName("Action Controller Tests")
@Tag("integration")
class ActionControllerTest extends AbstractSpringFixedEnvironmentIT {

  /**
   * Test of launchAction method, of class ActionController.
   *
   * @param method Clean up method
   * @throws Exception Test error
   */
  protected void cleanUp(String method) throws Exception {

    logger.debug("--------------------------------------------------------------------------------------");
    logger.debug(" Cleaning up all the mess... ");
    logger.debug("--------------------------------------------------------------------------------------");

    MvcResult mvcResult = mockMvc.perform(post("/action/maintain/" + method)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();
    String result = mvcResult.getResponse().getContentAsString();
    logger.debug(result);
  }

  @Nested
  @DisplayName("Login action tests")
  class ActionLoginControllerTest {

    @Test
    void testLoginWithValidCredentials() throws Exception {
      // Given
      String parameters = "{\"cod_usr\":\"test\",\"pwd_usr\":\"test\"}";
      // When
      MvcResult mvcResult = mockMvc.perform(post("/action/login")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(parameters)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].parameters.token", is(notNullValue())))
        .andReturn();
      // Then
      assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    @Test
    void testLoginWithNotValidParametersShouldShowArgumentsAreNotValid() throws Exception {
      // Given
      String parameters = "{\"cod_usr\":\"test\",\"pwd_usr\":null\"}";
      // When
      MvcResult mvcResult = mockMvc.perform(post("/action/login")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(parameters)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].parameters.type", is("warning")))
        .andExpect(jsonPath("$[1].parameters.message", is("The selected arguments are not valid")))
        .andReturn();
      // Then
      assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }
  }

  @Nested
  @DisplayName("Screen Data Controller Tests")
  class ScreenDataControllerTest {

    /**
     * Test of launchAction method, of class ActionController.
     *
     * @throws Exception Test error
     */
    @Test
    void testLaunchScreenDataAction() throws Exception {
      String expected = "[{\"type\":\"screen-data\",\"parameters\":{\"view\":\"base\",\"screenData\":{\"components\":[{\"id\":\"ButLogIn\",\"controller\":{\"actions\":[{\"type\":\"validate\"},{\"type\":\"server\",\"parameters\":{\"serverAction\":\"login\"}}],\"buttonType\":\"submit\",\"checkInitial\":true,\"checkTarget\":false,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"icon\":\"sign-in\",\"id\":\"ButLogIn\",\"label\":\"BUTTON_LOGIN\",\"optional\":false,\"printable\":true,\"readonly\":false,\"strict\":true,\"style\":\"no-class btn btn-primary signin-btn bg-primary\",\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}},{\"id\":\"pwd_usr\",\"controller\":{\"checkInitial\":true,\"checkTarget\":false,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"icon\":\"key signin-form-icon\",\"id\":\"pwd_usr\",\"optional\":false,\"placeholder\":\"SCREEN_TEXT_PASS\",\"printable\":true,\"readonly\":false,\"required\":true,\"size\":\"lg\",\"strict\":true,\"style\":\"no-label\",\"validation\":\"required\",\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}},{\"id\":\"cod_usr\",\"controller\":{\"checkInitial\":true,\"checkTarget\":false,\"checked\":false,\"contextMenu\":[],\"dependencies\":[],\"icon\":\"user signin-form-icon\",\"id\":\"cod_usr\",\"optional\":false,\"placeholder\":\"SCREEN_TEXT_USER\",\"printable\":true,\"readonly\":false,\"required\":true,\"size\":\"lg\",\"strict\":true,\"style\":\"no-label\",\"validation\":\"required\",\"visible\":true},\"model\":{\"selected\":[],\"defaultValues\":[],\"values\":[]}}],\"messages\":{},\"errors\":[],\"screen\":{\"name\":\"signin\",\"title\":\"SCREEN_TITLE_LOGIN\",\"option\":null}}}},{\"type\":\"end-load\"}]";
      MvcResult mvcResult = mockMvc.perform(post("/screen-data")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"view\":\"base\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
      logger.debug(expected);
      ObjectNode screenData = (ObjectNode) objectMapper.readTree(result);

      assertEquals(0, screenData.get("actions").size());
      assertEquals(0, screenData.get("messages").size());
      ArrayNode screenDataComponents = (ArrayNode) screenData.get("components");
      assertEquals(3, screenDataComponents.size());
      assertEquals("signin", screenData.get("screen").get("name").textValue());

      // Test all keys
      for (JsonNode element : screenDataComponents) {
        ObjectNode component = (ObjectNode) element;
        String key = component.get("id").asText();
        logger.debug(key + ": " + component.get("model").get("selected").toString());
      }

      logger.debug("-------------------------------------------");
      logger.debug("There are " + screenDataComponents.size() + " component in the screen " + screenData.get("screen").get("name"));
      logger.debug("-------------------------------------------");
    }

    /**
     * Test of launchAction method, of class ActionController.
     *
     * @throws Exception Test error
     */
    @Test
    void testLaunchScreenDataActionError() throws Exception {
      String expected = "{\"components\":[],\"messages\":{},\"template\":\"<div class=\\\"page-404 fullScreen\\\"><div class=\\\"header\\\"><a class=\\\"logo\\\"></a></div><div class=\\\"error-code\\\"></div><div class=\\\"error-text\\\"><span class=\\\"oops\\\"><span translate>404</span></span><span class=\\\"hr\\\"></span><span class=\\\"solve\\\"><span translate>404-DESCRIPTION</span></span></div></div>\\n<div></div>\\n<div class=\\\"hidden\\\"></div>\",\"actions\":[],\"screen\":{}}";
      MvcResult mvcResult = mockMvc.perform(post("/screen-data/pantalla-inexistente")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"view\":\"base\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(content().json(expected))
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
    }
  }

  @Nested
  @DisplayName("Locales Controller Tests")
  class LocalesControllerTest {

    /**
     * Test of launchAction method, of class ActionController.
     *
     * @throws Exception Test error
     */
    @Test
    void testLaunchGetLocalsAction() throws Exception {

      MvcResult mvcResult = mockMvc.perform(post("/action/get-locals")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"language\":\"es\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();
      //logger.debug(result);
      ArrayNode resultList = (ArrayNode) objectMapper.readTree(result);

      ObjectNode endLoad = (ObjectNode) resultList.get(0);
      assertEquals("end-load", endLoad.get("type").textValue());

      ObjectNode localsRetrievedActionES = (ObjectNode) resultList.get(1);
      assertEquals("locals-retrieved", localsRetrievedActionES.get("type").textValue());
      ObjectNode localsRetrievedParametersES = (ObjectNode) localsRetrievedActionES.get("parameters");
      assertEquals(2, localsRetrievedParametersES.size());
      ObjectNode translationsES = (ObjectNode) localsRetrievedParametersES.get("translations");
      int translationEsSize = translationsES.size();

      mockMvc.perform(post("/action/get-locals")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"language\":\"en\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

      ObjectNode localsRetrievedActionEN = (ObjectNode) resultList.get(1);
      assertEquals("locals-retrieved", localsRetrievedActionEN.get("type").textValue());
      ObjectNode localsRetrievedParametersEN = (ObjectNode) localsRetrievedActionEN.get("parameters");
      assertEquals(2, localsRetrievedParametersEN.size());
      ObjectNode translationsEN = (ObjectNode) localsRetrievedParametersEN.get("translations");
      assertEquals(translationEsSize, translationsEN.size());

      mockMvc.perform(post("/action/get-locals")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"language\":\"fr\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

      ObjectNode localsRetrievedActionFR = (ObjectNode) resultList.get(1);
      assertEquals("locals-retrieved", localsRetrievedActionFR.get("type").textValue());
      ObjectNode localsRetrievedParametersFR = (ObjectNode) localsRetrievedActionFR.get("parameters");
      assertEquals(2, localsRetrievedParametersFR.size());
      ObjectNode translationsFR = (ObjectNode) localsRetrievedParametersFR.get("translations");
      assertEquals(translationEsSize, translationsFR.size());

      // Test all keys
      Map<String, String> translationsMap = DataListUtil.getMapper().convertValue(translationsES, Map.class);
      List<String> keys = new ArrayList<>(translationsMap.keySet());
      Collections.sort(keys);

      for (String key : keys) {
        assertTrue(translationsEN.has(key));
        assertTrue(translationsFR.has(key));
      }

      logger.debug("-------------------------------------------");
      logger.debug("There are " + translationEsSize + " locales");
      logger.debug("-------------------------------------------");
    }
  }

  @Nested
  @DisplayName("Screen Restrictions Tests")
  @WithMockUser(username = "test", password = "test")
  class ScreenRestrictionsTest {

    @BeforeEach
    public void setup() throws Exception {
      // Clean up
      cleanUp("CleanUpScreenRestriction");
    }

    @AfterEach
    public void clean() throws Exception {
      // Clean up
      cleanUp("CleanUpScreenRestriction");
    }

    /**
     * Test of screen restriction
     *
     * @throws Exception Test error
     */
    @Test
    void testRestrictHelpAll() throws Exception {
      String parameters = prepareOptions("images/flags/help", "R", null);
      testOptionRestriction(null, null, "images/flags/help", "R", parameters);
    }

    /**
     * Test of screen restriction
     *
     * @throws Exception Test error
     */
    @Test
    void testRestrictToolsAll() throws Exception {
      String parameters = prepareOptions("tools", "R", null);
      testOptionRestriction(null, null, "tools", "R", parameters);
    }

    /**
     * Test of screen restriction - Change label
     *
     * @throws Exception Test error
     */
    @Test
    void testSomeRestrictions() throws Exception {
      // Add restriction to settings
      String parameters = prepareOptions("settings", "R", null);
      testOptionRestriction(null, null, "settings", "R", parameters);

      // Remove restriction from security
      parameters = prepareOptions("security", "A", parameters);
      testOptionRestriction(null, null, "security", "A", parameters);
    }

    /**
     * Test of screen restriction
     *
     * @throws Exception Test error
     */
    @Test
    void testEncryptToolUser() throws Exception {
      // Add restriction to user 'pgarcia' and check with 'test'
      String parameters = prepareOptions(null, null, null);
      testOptionRestriction("3", null, "encrypt-tools", "R", parameters);

      // Add restriction to user 'pgarcia' and check with 'pgarcia'
      parameters = prepareOptions("encrypt-tools", "R", null);
      testOptionRestriction("3", null, "encrypt-tools", "R", parameters);
    }

    /**
     * Test of screen restriction
     *
     * @throws Exception Test error
     */
    @Test
    void testEncryptToolProfile() throws Exception {
      // Add restriction to profile 'administrator' and check with 'test'
      String parameters = prepareOptions("sites", "R", null);
      testOptionRestriction(null, "1", "sites", "R", parameters);

      // Add restriction to profile 'administrator' and check with 'dfuentes'
      testOptionRestriction(null, "1", "sites", "R", parameters);
    }

    /**
     * Restrict an option and its children
     *
     * @param option Option node
     */
    private void restrictOption(ObjectNode option, String optionName, boolean restrict) {
      String searchFor = optionName;
      if (option.has("name") && optionName.equalsIgnoreCase(option.get("name").textValue()) ||
        "*".equalsIgnoreCase(optionName)) {
        option.put("restricted", restrict);
        searchFor = "*";
      }

      if (option.has("options")) {
        ArrayNode options = (ArrayNode) option.get("options");
        for (JsonNode childNode : options) {
          ObjectNode childOption = (ObjectNode) childNode;
          restrictOption(childOption, searchFor, restrict);
        }
      }
    }

    /**
     * Add a restriction
     *
     * @throws Exception Test error
     */
    private void addRestriction(String operation, String user, String profile, String option, String value) throws Exception {
      String maintainName = "updateScreenRestriction";
      String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"INSERT\",\"rowsAffected\":1},{\"operationType\":\"AUDIT\",\"rowsAffected\":1}]}}]";
      MvcResult mvcResult = mockMvc.perform(post("/action/maintain/" + maintainName)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"module\":\"Test\",\"language\":\"en\",\"RefreshTime\":null,\"site\":\"Madrid\",\"database\":\"awemadora01\",\"theme\":\"sunset\",\"SetAutoload\":\"0\",\"GrdScrAccLst\":1,\"GrdScrAccLst.data\":{\"max\":30,\"page\":1,\"sort\":[]},\"IdeAweScrRes\":[\"\"],\"IdeAweScrRes.selected\":null,\"Opt\":[\"" + option + "\"],\"Opt.selected\":null,\"IdeOpe\":[" + user + "],\"IdeOpe.selected\":null,\"IdePro\":[" + profile + "],\"IdePro.selected\":null,\"AccMod\":[\"" + value + "\"],\"AccMod.selected\":null,\"Act\":[\"1\"],\"Act.selected\":null,\"GrdScrAccLst-id\":[\"new-row-1\"],\"GrdScrAccLst-RowTyp\":[\"" + operation + "\"],\"PrnScr\":\"ScrCnf\",\"CrtScr\":null,\"UsrPrn\":null,\"ActPrn\":\"2\",\"FmtPrn\":\"PDF\",\"CrtAct\":null,\"CrtUsr\":null,\"DblFmtPrn\":\"0\",\"TypPrn\":\"1\",\"CrtPro\":null,\"max\":30}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(expected))
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();

      assertResultJson(maintainName, result, 2, new MaintainResultDetails[]{
        new MaintainResultDetails(MaintainType.valueOf(operation), 1L),
        new MaintainResultDetails(MaintainType.AUDIT, 1L)
      });
    }

    /**
     * Test of option restriction
     *
     * @return parameters
     * @throws Exception Test error
     */
    private String prepareOptions(String optionName, String value, String previousOptions) throws Exception {
      // Define button controller
      String options = previousOptions != null ? previousOptions : TestUtil.readFileAsText("menu/menu.json");

      // Filter options
      if (optionName != null) {
        ObjectNode optionsObject = (ObjectNode) objectMapper.readTree(options);
        restrictOption(optionsObject, optionName, "R".equalsIgnoreCase(value));
        options = optionsObject.toString();
      }
      return options;
    }

    /**
     * Test of option restriction
     *
     * @throws Exception Test error
     */
    private void testOptionRestriction(String user, String profile, String optionName, String value, String parameters) throws Exception {

      // Add restriction
      addRestriction("INSERT", user, profile, optionName, value);

      // Check screen
      String expected = "[{\"type\":\"change-menu\",\"parameters\":" + parameters + "},{\"type\":\"end-load\"}]";
      MvcResult mvcResult = mockMvc.perform(post("/action/refresh-menu")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"view\":\"report\"}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
      logger.debug(expected);
    }
  }

  @Nested
  @DisplayName("Screen Configuration Tests")
  @WithMockUser(username = "test", password = "test")
  class ScreenConfigurationTest {

    // Initialize parameters
    private final String screenParameters = "{\"s\":\"e6144dad-6e67-499e-b74a-d1e600732e11\",\"option\":\"criteria-test-left\",\"view\":\"report\",\"TxtRea\":\"15:06:23\"}";

    @BeforeEach
    public void setup() throws Exception {
      // Clean up
      cleanUp("CleanUpScreenConfiguration");
    }

    @AfterEach
    public void clean() throws Exception {
      // Clean up
      cleanUp("CleanUpScreenConfiguration");
    }

    /**
     * Add a restriction
     *
     * @throws Exception Test error
     */
    private void addRestriction(String operation, String screen, String component, String attribute, String value) throws Exception {
      String maintainName = "updateScreenConfiguration";
      String expected = "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"" + operation + "\",\"rowsAffected\":1}],\"title\":\"Operation successful\",\"type\":\"ok\"}}]";
      MvcResult mvcResult = mockMvc.perform(post("/action/maintain/" + maintainName)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"module\":\"Test\",\"language\":\"en\",\"RefreshTime\":null,\"site\":\"Madrid\",\"database\":\"awemadora01\",\"theme\":\"sunset\",\"SetAutoload\":\"0\",\"GrdScrCnf\":1,\"GrdScrCnf.data\":{\"max\":30,\"page\":1,\"sort\":[]},\"IdeAweScrCnf\":[\"\"],\"IdeAweScrCnf.selected\":null,\"Scr\":[\"" + screen + "\"],\"Scr.selected\":null,\"IdeOpe\":[\"\"],\"IdeOpe.selected\":null,\"IdePro\":[\"\"],\"IdePro.selected\":null,\"Nam\":[\"" + component + "\"],\"Nam.selected\":null,\"Atr\":[\"" + attribute + "\"],\"Atr.selected\":null,\"Val\":[\"" + value + "\"],\"Val.selected\":null,\"Act\":[\"1\"],\"Act.selected\":null,\"GrdScrCnf-id\":[\"new-row-1\"],\"GrdScrCnf-RowTyp\":[\"" + operation + "\"],\"PrnScr\":\"ScrCnf\",\"CrtScr\":null,\"UsrPrn\":null,\"ActPrn\":\"2\",\"FmtPrn\":\"PDF\",\"CrtAct\":null,\"CrtUsr\":null,\"DblFmtPrn\":\"0\",\"TypPrn\":\"1\",\"CrtPro\":null,\"max\":30}")
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(expected))
        .andReturn();
      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
      assertResultJson(maintainName, result, 1, new MaintainResultDetails[]{
        new MaintainResultDetails(MaintainType.valueOf(operation), 1L)
      });
    }


    /**
     * Test of screen restriction - Hide criterion
     *
     * @throws Exception Test error
     */
    @Test
    void testRestrictVisibility() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "ButBck", "visible", "false");
      // Check screen
      checkAttributeComponent("ButBck", "visible", false);
    }

    /**
     * Test of screen restriction - Restricted values of criterion
     *
     * @throws Exception Test error
     */
    @Test
    void testRestrictedValues() throws Exception {

      addRestriction("INSERT", "CrtTstLeft", "SelRea", "restrictedValueList", "0");

      // Check screen
      mockMvc.perform(post("/screen-data/criteria-test-left")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(screenParameters)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components[?(@.id == 'SelRea')].model.records", is(Collections.singletonList(1))))
        .andExpect(jsonPath("$.components[?(@.id == 'SelRea')].model.values[0].label", is(Collections.singletonList("ENUM_YES"))))
        .andExpect(jsonPath("$.components[?(@.id == 'SelRea')].model.values[0].value", is(Collections.singletonList("1"))))
        .andReturn();
    }

    /**
     * Test of screen restriction - Change label
     *
     * @throws Exception Test error
     */
    @Test
    void testChangeLabel() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "Txt", "label", "Tira patraaaaas");
      // Check screen
      checkAttributeComponent("Txt", "label", "Tira patraaaaas");
    }

    /**
     * Test of screen restriction - Change label
     *
     * @throws Exception Test error
     */
    @Test
    void testSetReadonly() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "Txt", "readonly", "true");
      // Check screen
      checkAttributeComponent("Txt", "readonly", true);
    }

    /**
     * Test of screen restriction - Optional
     *
     * @throws Exception Test error
     */
    @Test
    void testOptional() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "Txt", "optional", "true");
      // Check screen
      checkAttributeComponent("Txt", "optional", true);
    }

    /**
     * Test of screen restriction - Printable
     *
     * @throws Exception Test error
     */
    @Test
    void testPrintable() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "ButBck", "printable", "false");

      // Check screen
      checkAttributeComponent("ButBck", "printable", false);
    }

    /**
     * Test of screen restriction - Checked
     *
     * @throws Exception Test error
     */
    @Test
    void testChecked() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "ButBck", "checked", "true");

      // Check screen
      checkAttributeComponent("ButBck", "checked", true);
    }

    /**
     * Test of screen restriction - Help label
     *
     * @throws Exception Test error
     */
    @Test
    void testHelpLabel() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "ButBck", "help", "Ayudaaaaaaaaaaaaaaaa");

      // Check screen
      checkAttributeComponent("ButBck", "help", "Ayudaaaaaaaaaaaaaaaa");
    }

    /**
     * Test of screen restriction - Style
     *
     * @throws Exception Test error
     */
    @Test
    void testStyle() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "ButBck", "style", "claseDeLaMuerte otraClaseChula");

      // Check screen
      checkAttributeComponent("ButBck", "style", "claseDeLaMuerte otraClaseChula");
    }

    /**
     * Test of screen restriction - Initial load
     *
     * @throws Exception Test error
     */
    @Test
    void testInitialLoad() throws Exception {
      // Add restrictions
      addRestriction("INSERT", "CrtTstLeft", "Txt", "initialLoad", "value");
      addRestriction("INSERT", "CrtTstLeft", "Txt", "targetAction", "TestComponentInitialLoadValue");

      MvcResult mvcResult = mockMvc.perform(post("/screen-data/criteria-test-left")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(screenParameters)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components[?(@.id == 'Txt')].controller.initialLoad", is(Collections.singletonList("value"))))
        .andExpect(jsonPath("$.components[?(@.id == 'Txt')].controller.targetAction", is(Collections.singletonList("TestComponentInitialLoadValue"))))
        .andExpect(jsonPath("$.components[?(@.id == 'Txt')].model.records", is(Collections.singletonList(1))))
        .andReturn();

      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
    }

    /**
     * Test of screen restriction - Style
     *
     * @throws Exception Test error
     */
    @Test
    void testAttributeNotForComponent() throws Exception {
      // Add restriction
      addRestriction("INSERT", "CrtTstLeft", "Txt", "editable", "true");

      // Check screen
      checkAttributeComponentDoesntExist("Txt", "editable");
    }


    /**
     * Perform screen data action to check Json controller attributes
     *
     * @param componentId component ID
     * @param attribute   component attribute
     * @param value       attribute value
     * @throws Exception UnsupportedEncodingException exception
     */
    private void checkAttributeComponent(String componentId, String attribute, Object value) throws Exception {
      checkAttributeComponentMatcher(componentId, attribute, is(Collections.singletonList(value)));
    }

    /**
     * Perform screen data action to check Json controller attributes
     *
     * @param componentId component ID
     * @param attribute   component attribute
     * @throws Exception UnsupportedEncodingException exception
     */
    private void checkAttributeComponentDoesntExist(String componentId, String attribute) throws Exception {
      checkAttributeComponentMatcher(componentId, attribute, is(Collections.EMPTY_LIST));
    }

    private void checkAttributeComponentMatcher(String componentId, String attribute, Matcher matcher) throws Exception {
      // Get screen action and check attribute
      MvcResult mvcResult = mockMvc.perform(post("/screen-data/criteria-test-left")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(screenParameters)
          .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath(String.format("$.components[?(@.id == '%s')].controller.%s", componentId, attribute), matcher))
        .andReturn();

      String result = mvcResult.getResponse().getContentAsString();
      logger.debug(result);
    }
  }
}
