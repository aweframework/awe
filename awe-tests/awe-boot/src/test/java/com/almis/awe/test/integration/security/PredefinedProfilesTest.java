package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The predefined profiles Reader and Contributor are created at startup
 */
@Tag("integration")
@DisplayName("Predefined profiles tests")
class PredefinedProfilesTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("The Reader and Contributor profiles exist after the startup, and ProKey is past their identifiers")
  void readerAndContributorAreCreatedAtStartup() {
    List<Map<String, Object>> profiles = jdbcTemplate.queryForList(
      "SELECT Acr, Nam, Res, Act FROM AwePro WHERE Acr IN ('RDR', 'CTB') ORDER BY Acr");

    assertThat(profiles).extracting(row -> row.get("ACR") + " " + row.get("NAM") + " " + row.get("RES") + " "
      + row.get("ACT")).containsExactly("CTB Contributor contributor 1", "RDR Reader reader 1");
    assertThat(jdbcTemplate.queryForObject("SELECT KeyVal FROM AweKey WHERE KeyNam = 'ProKey'", Integer.class))
      .isGreaterThanOrEqualTo(jdbcTemplate.queryForObject("SELECT MAX(IdePro) FROM AwePro", Integer.class));
  }
}
