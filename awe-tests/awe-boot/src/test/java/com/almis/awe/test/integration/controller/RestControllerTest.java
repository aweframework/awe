package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("Rest controller Tests")
@Slf4j
class RestControllerTest extends AbstractSpringFixedEnvironmentIT {

  @Nested
  @DisplayName("Microservices Tests")
  @WithMockUser(username = "test", password = "test")
  class MicroServiceTest {

    private static final String SESSION_ID = "16617f0d-97ee-4f6b-ad54-905d6ce3c328";
    private MockHttpSession session;

    /**
     * Initializes json mapper for tests
     **/
    @BeforeEach
    public void setup() {
      session = new MockHttpSession(null, SESSION_ID);
    }

    /**
     * Simple microservice call
     *
     * @throws Exception Test error
     */
    @Test
    void testSimpleMicroservice() throws Exception {
      setParameter("database", "awedb01");
      setParameter("currentDate", "22/02/2019");
      doRestTest("CallAluMicroservice", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple microservice call to another microservice
     *
     * @throws Exception Test error
     */
    @Test
    void testAnotherMicroservice() throws Exception {
      doRestTest("CallAnotherMicroservice", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"rdb\":\"\",\"null\":null,\"double\":22.0,\"text\":\"test\",\"integer\":22,\"id\":1,\"float\":22.0,\"long\":22}]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple microservice call without parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testSimpleMicroserviceWithoutParameters() throws Exception {
      setParameter("database", "awedb01");
      setParameter("currentDate", "22/02/2019");
      doRestTest("CallAluMicroserviceWithoutParameters", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple microservice call to another microservice
     *
     * @throws Exception Test error
     */
    @Test
    void testAnotherMicroserviceWithParameters() throws Exception {
      doRestTest("CallAnotherMicroserviceWithParameters", "data", "\"tutu\":\"23/10/1978\", \"lala\":[1,2,4], \"erre\": \"\", \"queErre\": null, \"yQueErre\": null,", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"rdb\":\"01-JAN-2019\",\"floatFormatted\":\"22\",\"dateFormatted\":\"23/10/1978\",\"longFormatted\":\"22\",\"doubleFormatted\":\"22\",\"null\":null,\"text\":\"test\",\"id\":1,\"integerFormatted\":\"22\"}]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple microservice call to another microservice
     *
     * @throws Exception Test error
     */
    @Test
    void testAnotherMoreMicroservice() throws Exception {
      doRestTest("CallAnotherMoreMicroservice", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple call with overwrite microservice name
     *
     * @throws Exception Test error
     */
    @Test
    void overwriteMicroserviceNameTest() throws Exception {
      doRestTest("CallOverWriteMicroserviceName", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}}]");
    }

    /**
     * Simple call with overwrite microservice name returning an AWException
     */
    @Test
    void testMicroserviceCallError() throws Exception {
      doRestTest("CallOverWriteMicroserviceNameError", "data", "", "[{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"message\",\"parameters\":{\"type\":\"error\",\"title\":\"Error title\",\"message\":\"Error message\"}}]");
    }

    /**
     * Simple call with overwrite microservice name returning an AWException
     */
    @Test
    void testMicroserviceCallWarning() throws Exception {
      doRestTest("CallOverWriteMicroserviceNameWarning", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"message\",\"parameters\":{\"type\":\"warning\",\"title\":\"Warning title\",\"message\":\"Warning message\"}}]");
    }

    /**
     * Simple call with overwrite microservice name returning an AWException
     */
    @Test
    void testMicroserviceCallInfo() throws Exception {
      doRestTest("CallOverWriteMicroserviceNameInfo", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\",\"parameters\":{}},{\"type\":\"message\",\"parameters\":{\"type\":\"info\",\"title\":\"Info title\",\"message\":\"Info message\"}}]");
    }

    /**
     * Test a REST POST
     *
     * @param name       Target action
     * @param action     Server action
     * @param parameters Extra parameters
     * @param expected   Expected result
     * @throws Exception Exception
     */
    private void doRestTest(String name, String action, String parameters, String expected) throws Exception {

      MvcResult mvcResult = mockMvc.perform(post("/action/" + action + "/" + name)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{" + parameters + "\"max\":30}")
              .accept(MediaType.APPLICATION_JSON)
              .session(session))
              .andExpect(status().isOk())
              .andExpect(content().json(expected))
              .andReturn();
    }

    /**
     * Set parameter in session
     *
     * @param name  Parameter name
     * @param value Parameter value
     */
    private void setParameter(String name, String value) throws Exception {
      MvcResult mvcResult = mockMvc.perform(post(String.format("/session/set/%s", name))
              .with(csrf())
              .param("value", value)
              .session(session))
              .andReturn();
      mvcResult.getResponse().getContentAsString();
    }

  }

  @Nested
  @DisplayName("Rest services Tests")
  @WithMockUser(username = "test", password = "test")
  class RestServiceTest {

    /**
     * Rest test: Simple get query
     *
     * @throws Exception Test error
     */
    @Test
    void testSimpleGetData() throws Exception {
      doRestTest("TestSimpleRestGet", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\"}]");
    }

    /**
     * Rest test: Simple post query
     *
     * @throws Exception Test error
     */
    @Test
    void testSimplePostData() throws Exception {
      doRestTest("TestSimpleRestPost", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":0,\"rows\":[]}}},{\"type\":\"end-load\"}]");
    }

    /**
     * Rest test: Simple get maintain
     *
     * @throws Exception Test error
     */
    @Test
    void testSimpleGetMaintain() throws Exception {
      doRestTest("TestSimpleRestGet", "maintain", "", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Rest test: Simple post maintain
     *
     * @throws Exception Test error
     */
    @Test
    void testSimplePostMaintain() throws Exception {
      doRestTest("TestSimpleRestPost", "maintain", "", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Rest test: Complex get
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexGet() throws Exception {
      doRestTest("TestComplexRestGet", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"CrtTst\":1,\"id\":1}]}}},{\"type\":\"end-load\"}]");
    }

    /**
     * Rest test: Complex post
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexPost() throws Exception {
      doRestTest("TestComplexRestPost", "maintain", "", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"DELETE\",\"rowsAffected\":1,\"parameterMap\":{\"Action\":\"T\",\"User\":\"testIncludeTarget\",\"_page_\":1,\"_max_\":30}}]}}]");
    }

    /**
     * Rest test: Complex get with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexGetParameters() throws Exception {
      doRestTest("TestComplexRestGetParameters", "data", "\"value\":\"1\",", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"CrtTst\":1,\"id\":1}]}}},{\"type\":\"end-load\"}]");
    }

    /**
     * Rest test: Complex post with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexPostParameters() throws Exception {
      doRestTest("TestComplexRestPostParameters", "maintain", "\"value\":1,", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"DELETE\",\"rowsAffected\":1,\"parameterMap\":{\"Action\":\"T\",\"User\":\"testRestTarget\",\"_page_\":1,\"_max_\":30}}]}}]");
    }

    /**
     * Rest test: Complex post with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexPostParametersJson() throws Exception {
      doRestTest("TestComplexRestPostParametersJson", "maintain", "\"value\":1,", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[{\"operationType\":\"DELETE\",\"rowsAffected\":1,\"parameterMap\":{\"Action\":\"T\",\"User\":\"testRestTarget\",\"_page_\":1,\"_max_\":30}}]}}]");
    }

    /**
     * Rest test: Complex post with POJO
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexPostParametersPOJO() throws Exception {
      doRestTest("TestComplexJavaParametersPOJO", "maintain", "\"name\":\"my concert\",\"userGroupName\":\"user group\",\"eventHallName\":\"great hall\",", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Rest test: Complex post with POJO list
     *
     * @throws Exception Test error
     */
    @Test
    void testComplexPostParametersPOJOList() throws Exception {
      doRestTest("TestComplexJavaParametersPOJOList", "maintain", "\"dates\":[\"10/02/1985\", \"11/02/1985\"],\"name\":\"my concert\",\"userGroupName\":\"user group\",\"eventHallName\":\"great hall\",", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Call a external rest API
     *
     * @throws Exception Test error
     */
    @Test
    void testExternalRestApi() throws Exception {
      doRestTest("TestExternalRestApi", "data", "\"value\":3,", null);
    }

    /**
     * Call a external rest API
     *
     * @throws Exception Test error
     */
    @Test
    void testExternalRestApiWithSecurity() throws Exception {
      doRestTest("TestExternalRestApiWithSecurity", "data", "\"value\":3,", null);
    }

    /**
     * Call a external rest API
     *
     * @throws Exception Test error
     */
    @Test
    @Tag(value = "CIDatabase")
    void testPostmanRestApi() throws Exception {
      doRestTest("TestPostmanRestApi", "data", "", "[{\"type\":\"fill\",\"parameters\":{\"datalist\":{\"total\":1,\"page\":1,\"records\":1,\"rows\":[{\"acceptLanguage\":\"\",\"acceptEncoding\":\"gzip, br\",\"cookie\":\"\",\"method\":\"GET\",\"gzipped\":true,\"postmanToken\":\"\",\"id\":1,\"cacheControl\":\"\",\"accept\":\"application/json, application/*+json\"}]}}},{\"type\":\"end-load\"}]");
    }

    /**
     * Rest test: Complex post with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testPostParameterList() throws Exception {
      doRestTest("TestComplexRestPostParametersList", "maintain", "\"stringList\":[\"tutu\", \"lala\", \"yoyo\"],\"integerList\":[4, 6, 7],\"dateList\":[\"23/04/2014\", \"22/05/2017\", \"07/01/2019\"],", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Rest test: Complex post with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testPostParameterListGetParameters() throws Exception {
      doRestTest("TestComplexRestPostParametersListGetParameters", "maintain", "\"stringList\":[\"tutu\", \"lala\", \"yoyo\"],\"integerList\":[4, 6, 7],\"dateList\":[\"23/04/2014\", \"22/05/2017\", \"07/01/2019\"],", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Rest test: Complex post with parameters
     *
     * @throws Exception Test error
     */
    @Test
    void testPostParameterListJson() throws Exception {
      doRestTest("TestComplexRestPostParametersListJson", "maintain", "\"stringList\":[\"tutu\", \"lala\", \"yoyo\"],\"integerList\":[4, 6, 7],\"dateList\":[\"23/04/2014\", \"22/05/2017\", \"07/01/2019\"],", "[{\"type\":\"end-load\"},{\"type\":\"message\",\"parameters\":{\"type\":\"ok\",\"title\":\"Operation successful\",\"message\":\"The selected maintain operation has been successfully performed\",\"result_details\":[]}}]");
    }

    /**
     * Test a REST POST
     *
     * @param name       Target action
     * @param action     Server action
     * @param parameters Extra parameters
     * @param expected   Expected result
     * @throws Exception Exception
     */
    private void doRestTest(String name, String action, String parameters, String expected) throws Exception {
      MvcResult mvcResult = mockMvc.perform(post("/action/" + action + "/" + name)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{" + parameters + "\"max\":30}")
              .accept(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andReturn();
      String result = mvcResult.getResponse().getContentAsString();

      // Check expected
      if (expected != null) {
        JSONAssert.assertEquals(expected, result, false);
      }
    }
  }
}
