package com.almis.awe.test.integration.security;

import com.almis.awe.factory.WithMockCustomUser;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@DisplayName("Security Tests")
class SecurityIntegrationTest extends AbstractSpringAppIntegrationTest {

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePublicQuery_should200Ok() throws Exception {
    mockMvc.perform(post("/action/data/GetCountries")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePrivateQuery_should401Unauthorized() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_executePublicMaintain_should200Ok() throws Exception {
    mockMvc.perform(post("/action/maintain/CleanUpSequence")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndValidCSRFToken_ExecutePrivateMaintain_should401NotAuthorized() throws Exception {
    mockMvc.perform(post("/action/maintain/CleanUp")
        .with(csrf())
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  @WithAnonymousUser
  void givenAnonymousUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf().useInvalidToken())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isForbidden());
  }

  @Test
  @WithMockCustomUser(username = "test")
  void givenAuthUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mockMvc.perform(post("/action/data/SimpleGetAll")
        .with(csrf().useInvalidToken())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isForbidden());
  }

  @Test
  void givenInvalidJWTToken_shouldUnauthorized401() throws Exception {
    mockMvc.perform(post("/api/data/SimpleGetAll")
        .header("Authorization", "invalid-token")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void givenNullJWTToken_shouldUnauthorized401() throws Exception {
    mockMvc.perform(post("/api/data/SimpleGetAll")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }

  @Test
  void givenUserCallGetAuthenticate_shouldMethodNowAllowed405() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/authenticate")
        .queryParam("username", "test")
        .queryParam("password", "test")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isMethodNotAllowed());
  }

}
