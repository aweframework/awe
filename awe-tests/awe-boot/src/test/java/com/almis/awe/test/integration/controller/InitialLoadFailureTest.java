package com.almis.awe.test.integration.controller;

import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A screen whose initial load fails is returned with the error message, and the request thread is not left
 * interrupted: Tomcat 11 cannot write the response on an interrupted thread (#894)
 */
@DisplayName("Initial load failure")
@Tag("integration")
@WithMockUser(username = "test", password = "test")
@TestPropertySource(properties = "awe.test.context=initial-load-failure")
class InitialLoadFailureTest extends AbstractSpringAppIntegrationTest {

  private static final String SCREEN_PARAMETERS = "{\"module\":\"Test\",\"option\":\"criteria-test-left\",\"view\":\"report\"}";

  @MockitoSpyBean
  private InitialLoadDao initialLoadDao;

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void failedInitialLoadReturnsTheScreenWithAnErrorWithoutInterruptingTheRequestThread() throws Exception {
    MockHttpSession session = new MockHttpSession();
    setSessionParameter(session, "user", "test");
    setSessionParameter(session, "restriction", "administrator");
    // Stub the spy behind the @Async proxy: the proxy runs it on the executor, as a real failing load
    InitialLoadDao spy = AopTestUtils.getUltimateTargetObject(initialLoadDao);
    doAnswer(invocation -> CompletableFuture.failedFuture(new IllegalStateException("Connection refused")))
      .when(spy).launchInitialLoad(any());

    String response = mockMvc.perform(post("/screen-data/criteria-test-left")
        .session(session)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(SCREEN_PARAMETERS)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    JsonNode screenData = objectMapper.readTree(response);
    List<String> actionTypes = new ArrayList<>();
    screenData.get("actions").forEach(action -> actionTypes.add(action.get("type").asString()));
    assertThat(screenData.get("components")).isNotEmpty();
    assertThat(actionTypes).contains("message");
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  private void setSessionParameter(MockHttpSession session, String name, String value) throws Exception {
    mockMvc.perform(post(String.format("/session/set/%s", name))
        .with(csrf())
        .param("value", value)
        .session(session))
      .andExpect(status().isOk());
  }
}
