package com.almis.awe.security.handler;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.audit.RecordingSecurityAudit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class AweAccessDeniedHandlerTest {

  @BeforeEach
  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void forbiddenRequestsAreAudited() throws Exception {
    RecordingSecurityAudit audit = new RecordingSecurityAudit();
    AweAccessDeniedHandler handler = new AweAccessDeniedHandler();
    handler.setSecurityAuditService(audit.service());
    SecurityContextHolder.setContext(new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated("jdoe", null, AuthorityUtils.NO_AUTHORITIES)));
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.handle(new MockHttpServletRequest("GET", "/action/data/secret"), response, new AccessDeniedException("no"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(audit.events()).extracting("type", "outcome", "cause", "username", "detail").containsExactly(
      tuple(SecurityAuditEventType.ACCESS_DENIED, SecurityAuditOutcome.FAILURE, SecurityAuditCause.FORBIDDEN, "jdoe", "url: /action/data/secret"));
  }
}
