package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

  @Mock
  private LauncherService launcherService;

  private AweRequest request;
  private TestableActionService actionService;

  @BeforeEach
  void setUp() {
    request = new AweRequest(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new ObjectMapper());
    actionService = new TestableActionService(launcherService, new BaseConfigProperties());
    actionService.setTestRequest(request);
  }

  @Test
  void shouldFormatScalarStringsWithQuotesAndPreserveJsonStructures() {
    request.setParameter("selectedIds", "10", "20");
    request.setParameter("pwd_usr", "secret");
    request.setParameter("description", "contains secret value");
    request.setParameter("status", "active");
    request.setParameter("empty", "");
    request.setParameter("attempts", 3);

    String formattedParameters = ReflectionTestUtils.invokeMethod(actionService, "getParameterListAsString");

    assertThat(formattedParameters)
      .contains("selectedIds=[\"10\",\"20\"]")
      .contains("pwd_usr=*****")
      .contains("description=\"contains ***** value\"")
      .contains("status=\"active\"")
      .contains("empty=\"\"")
      .contains("attempts=3");
  }

  static class TestableActionService extends ActionService {
    private AweRequest testRequest;

    TestableActionService(LauncherService launcherService, BaseConfigProperties baseConfigProperties) {
      super(launcherService, baseConfigProperties);
    }

    void setTestRequest(AweRequest testRequest) {
      this.testRequest = testRequest;
    }

    @Override
    public AweRequest getRequest() {
      return testRequest;
    }
  }
}
