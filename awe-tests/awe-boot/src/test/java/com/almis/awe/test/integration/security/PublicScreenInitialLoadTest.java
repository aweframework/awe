package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The initial loads of a screen are checked as client launches, but a public screen opened without a session keeps
 * loading the private queries that only the public screens use, as before the check
 */
@Tag("integration")
@DisplayName("Public screen initial load tests")
@WithAnonymousUser
@TestPropertySource(properties = {"awe.security.authorization.mode=enforce", "awe.security.default-restriction=operator"})
class PublicScreenInitialLoadTest extends AbstractSpringAppIntegrationTest {

  @Test
  @DisplayName("A public screen opened without a session loads its private query without the restricted data warning")
  void anonymousPublicScreenLoadsItsPrivateQuery() throws Exception {
    String response = mockMvc.perform(post("/screen-data/test-public-screen-private-load")
        .with(csrf())
        .content("{\"view\":\"base\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<List<Object>>read(response, "$.actions[?(@.type == 'message')]")).as(response).isEmpty();
    assertThat(JsonPath.<Integer>read(response, "$.messages.length()")).as(response).isZero();
    assertThat(JsonPath.<List<Integer>>read(response,
      "$.components[?(@.id == 'PrivateQuerySelect')].model.values.length()"))
      .as(response).singleElement().satisfies(values -> assertThat(values).isPositive());
  }
}
