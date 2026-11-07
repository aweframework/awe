package com.almis.awe.test.integration.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The REST API is authorized by the permissions only: a user that cannot open any screen that uses a target launches it
 * with the permission of the target, and a launch without the permission answers 403 with the generic refusal.
 */
@Tag("integration")
@DisplayName("REST API authorization tests")
@TestPropertySource(properties = {"awe.security.authorization.mode=enforce", "awe.security.default-restriction=operator"})
class RestApiAuthorizationTest extends ScreenAccessSupport {

  private static final String USER = "restreader";
  private static final String ACRONYM = "Q8R";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;
  private int userId;

  @BeforeEach
  void createUsersReader() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    deleteRows();
    int profile = jdbcTemplate.queryForObject("SELECT MAX(IdePro) FROM AwePro", Integer.class) + 1000;
    jdbcTemplate.update("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (?, ?, ?, 1, NULL, "
      + "'general', 1)", profile, ACRONYM, USER);
    int permission = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(IdeAweProPer), 0) FROM AweProPer",
      Integer.class) + 1;
    jdbcTemplate.update("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) VALUES (?, ?, 'users.read')", permission,
      profile);
    userId = support.copyTestUser(USER);
    jdbcTemplate.update("UPDATE ope SET IdePro = ?, Enable2fa = 0 WHERE IdeOpe = ?", profile, userId);
  }

  @AfterEach
  void deleteRows() throws Exception {
    support.deleteUser(USER);
    jdbcTemplate.update("DELETE FROM AweProPer WHERE IdePro IN (SELECT IdePro FROM AwePro WHERE Acr = ?)", ACRONYM);
    jdbcTemplate.update("DELETE FROM AwePro WHERE Acr = ?", ACRONYM);
  }

  @Test
  @DisplayName("A user without access to the users screen launches its query through the REST API with users.read")
  void permissionAllowsTheQueryWithoutTheScreen() throws Exception {
    MockHttpServletResponse response = api("/api/data/UsrStaCnt");

    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.getContentAsString()).doesNotContain(REFUSED);
  }

  @Test
  @DisplayName("A launch through the REST API without the permission of the target answers 403 and is audited")
  void missingPermissionAnswersForbidden() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    MockHttpServletResponse response = api("/api/maintain/UsrDel");

    String body = response.getContentAsString();
    assertThat(response.getStatus()).as(body).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(JsonPath.<String>read(body, "$.type")).isEqualTo("ERROR");
    assertThat(JsonPath.<String>read(body, "$.title")).isEqualTo("Access denied");
    assertThat(JsonPath.<String>read(body, "$.message")).isEqualTo(REFUSED);
    assertThat(body).doesNotContain("UsrDel");
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ope WHERE IdeOpe = ?", Integer.class, userId))
      .isOne();
    assertThat(support.events(USER, start)).contains("ACCESS_DENIED/FAILURE/PERMISSION/REST");
  }

  private MockHttpServletResponse api(String path) throws Exception {
    return mockMvc.perform(post(path)
        .header("Authorization", "Bearer " + token(USER))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"parameters\":{\"IdeOpe\":[" + userId + "]}}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse();
  }
}
