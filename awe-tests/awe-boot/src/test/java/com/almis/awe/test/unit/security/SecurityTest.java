package com.almis.awe.test.unit.security;

import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Log4j2
public class SecurityTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Before
  public void setup() {
    mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
  }

  @Test
  @WithAnonymousUser
  public void givenAnonymousUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mvc.perform(post("/action/data/SimpleGetAll")
            .with(csrf().useInvalidToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "test")
  public void givenAuthUserAndInvalidCSRFToken_shouldForbidden403() throws Exception {
    mvc.perform(post("/action/data/SimpleGetAll")
            .with(csrf().useInvalidToken())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
  }

  @Test
  public void givenInvalidJWTToken_shouldUnauthorized401() throws Exception {
    mvc.perform(post("/api/data/SimpleGetAll")
            .header("Authorization", "invalid-token")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void givenNullJWTToken_shouldUnauthorized401() throws Exception {
    mvc.perform(post("/api/data/SimpleGetAll")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
  }

  @Test
  public void givenUserCallGetAuthenticate_shouldMethodNowAllowed405() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/authenticate")
            .queryParam("username", "test")
            .queryParam("password", "test")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isMethodNotAllowed());
  }

}
