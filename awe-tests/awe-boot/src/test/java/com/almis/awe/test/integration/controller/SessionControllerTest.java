package com.almis.awe.test.integration.controller;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Tag("integration")
@DisplayName("Session controller Tests")
@WithMockUser(username = "test", password = "test")
class SessionControllerTest extends AbstractSpringAppIntegrationTest {

  private static final String SESSION_ID = "16617f0d-97ee-4f6b-ad54-905d6ce3c328";
  private MockHttpSession session;

  @BeforeEach
  void setUp() {
    session = new MockHttpSession(null, SESSION_ID);
  }

  /**
   * Test that the setters to the session storage are working correctly.
   */
  @Test
  void testSetSessionValues() throws Exception {
    setParameter("parameter1", "value1");
    setParameter("parameter2", "value2");
    setParameter("parameter3", "value3");
    setParameter("parameter4", "value4");

    assertEquals("value1", getParameter("parameter1"));
    assertEquals("value2", getParameter("parameter2"));
    assertEquals("value3", getParameter("parameter3"));
    assertEquals("value4", getParameter("parameter4"));
  }

  /**
   * Test that the parameters from session are correctly modified
   */
  @Test
  void testModifySessionValues() throws Exception {
    setParameter("parameter1", "value4");
    setParameter("parameter2", "value3");
    setParameter("parameter3", "value2");
    setParameter("parameter4", "value1");
    assertAll(() -> {
      assertEquals("value4", getParameter("parameter1"));
      assertEquals("value3", getParameter("parameter2"));
      assertEquals("value2", getParameter("parameter3"));
      assertEquals("value1", getParameter("parameter4"));
    });

  }

  /**
   * Test that the parameters from session are correctly removed
   */
  @Test
  void testRemoveSessionValues() throws Exception {
    //Add values first
    testSetSessionValues();

    removeParameter("parameter1");
    removeParameter("parameter2");
    removeParameter("parameter3");
    removeParameter("parameter4");

    assertTrue(getParameter("parameter1").isEmpty());
    assertTrue(getParameter("parameter2").isEmpty());
    assertTrue(getParameter("parameter3").isEmpty());
    assertTrue(getParameter("parameter4").isEmpty());
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

  /**
   * Get parameter from session
   *
   * @param name Parameter name
   * @return Parameter value
   */
  private String getParameter(String name) throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/session/get/" + name)
            .with(csrf())
            .session(session))
            .andReturn();
    return mvcResult.getResponse().getContentAsString();
  }

  /**
   * Remove parameter from session
   *
   * @param name Parameter name
   */
  protected void removeParameter(String name) throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/session/remove/" + name)
            .with(csrf())
            .session(session))
            .andReturn();
    mvcResult.getResponse().getContentAsString();
  }
}
