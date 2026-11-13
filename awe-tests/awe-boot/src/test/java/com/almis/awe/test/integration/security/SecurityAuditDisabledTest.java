package com.almis.awe.test.integration.security;

import com.almis.awe.security.audit.SecurityAuditPurgeScheduler;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.security.audit.SecurityAuditSink;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * awe.security.audit.enabled=false switches the whole security audit off
 */
@Tag("integration")
@TestPropertySource(properties = "awe.security.audit.enabled=false")
@DisplayName("Security audit switched off tests")
class SecurityAuditDisabledTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private DataSource dataSource;

  @Test
  @DisplayName("No sink, no purge and no event is recorded")
  void nothingIsRecorded() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(new MockHttpSession())
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"test\",\"pwd_usr\":\"wrong-password\"}")
      .accept(MediaType.APPLICATION_JSON)).andReturn();

    assertThat(context.getBeansOfType(SecurityAuditSink.class)).isEmpty();
    assertThat(context.getBeansOfType(SecurityAuditPurgeScheduler.class)).isEmpty();
    assertThat(context.getBean(SecurityAuditService.class).isEnabled()).isFalse();
    assertThat(new SecurityAuditTestSupport(dataSource).events("test", start)).isEmpty();
  }
}
