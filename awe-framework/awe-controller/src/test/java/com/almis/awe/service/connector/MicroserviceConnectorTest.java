package com.almis.awe.service.connector;

import com.almis.awe.config.RestConfigProperties;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.rest.RestParameter;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MicroserviceConnectorTest {

  @Mock
  private ClientHttpRequestFactory requestFactory;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private RestConfigProperties restConfigProperties;
  @Mock
  private AweSession aweSession;
  @Mock
  private AweRequest aweRequest;

	private TestableMicroserviceConnector connector;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
		ObjectMapper objectMapper = new ObjectMapper();
    connector = new TestableMicroserviceConnector(requestFactory, queryUtil, objectMapper, restConfigProperties);
    connector.setTestSession(aweSession);
    connector.setTestRequest(null); // default null unless needed in tests
  }

  // SESSION branch
  @Test
  void getParameter_fromSession_returnsSessionValue() {
    // Given
    RestParameter param = new RestParameter();
    param.setName("p1");
    param.setValue("sessionKey");
    param.setType(RestParameter.ServiceParameterType.SESSION);

    when(aweSession.getParameter("sessionKey")).thenReturn("session-value");

    // When
    Object result = ReflectionTestUtils.invokeMethod(connector, "getParameter", param, new HashMap<String, Object>());

    // Then
    assertEquals("session-value", result);
  }

  // VARIABLE branch
  @Test
  void getParameter_fromVariable_returnsFromMap() {
    // Given
    RestParameter param = new RestParameter();
    param.setName("v1");
    param.setValue("varKey");
    param.setType(RestParameter.ServiceParameterType.VARIABLE);

    Map<String, Object> vars = new HashMap<>();
    vars.put("varKey", 123);

    // When
    Object result = ReflectionTestUtils.invokeMethod(connector, "getParameter", param, vars);

    // Then
    assertEquals(123, result);
  }

  // REQUEST branch when parameter present in request -> uses queryUtil.getRequestParameter
  @Test
  void getParameter_fromRequest_whenPresent_usesQueryUtil() {
    // Given
    RestParameter param = new RestParameter();
    param.setName("r1");
    param.setValue("reqKey");
    param.setType(RestParameter.ServiceParameterType.REQUEST);

    // Mock request with the parameter list having the key
    ObjectNode paramsNode = JsonNodeFactory.instance.objectNode();
    paramsNode.put("reqKey", "ignored-value-here");
    when(aweRequest.getParameterList()).thenReturn(paramsNode);
    connector.setTestRequest(aweRequest);

    // QueryUtil returns a JsonNode to be yielded
    ObjectNode expected = JsonNodeFactory.instance.objectNode();
    expected.put("foo", "bar");
    when(queryUtil.getRequestParameter(anyString())).thenReturn(expected);

    // When
    Object result = ReflectionTestUtils.invokeMethod(connector, "getParameter", param, new HashMap<String, Object>());

    // Then
    assertSame(expected, result, "Should return the JsonNode obtained from QueryUtil");
  }

  // REQUEST branch when parameter missing in request -> fallback to variableList
  @Test
  void getParameter_fromRequest_whenMissing_fallsBackToVariableList() {
    // Given
    RestParameter param = new RestParameter();
    param.setName("r2");
    param.setValue("reqKey2");
    param.setType(RestParameter.ServiceParameterType.REQUEST);

    // Mock request without the key
    ObjectNode paramsNode = JsonNodeFactory.instance.objectNode();
    when(aweRequest.getParameterList()).thenReturn(paramsNode);
    connector.setTestRequest(aweRequest);

    Map<String, Object> vars = new HashMap<>();
    vars.put("reqKey2", "fallback-value");

    // When
    Object result = ReflectionTestUtils.invokeMethod(connector, "getParameter", param, vars);

    // Then
    assertEquals("fallback-value", result);
  }

  // DEFAULT branch (VALUE) -> returns the static value string
  @Test
  void getParameter_defaultValue_returnsStaticValue() {
    // Given
    RestParameter param = new RestParameter();
    param.setName("p2");
    param.setValue("static-value");
    param.setType(RestParameter.ServiceParameterType.VALUE);

    // When
    Object result = ReflectionTestUtils.invokeMethod(connector, "getParameter", param, new HashMap<String, Object>());

    // Then
    assertEquals("static-value", result);
  }

  // Test helper subclass to inject session/request without Spring context
  @Setter
	static class TestableMicroserviceConnector extends MicroserviceConnector {
    private AweSession testSession;
    private AweRequest testRequest;

    public TestableMicroserviceConnector(ClientHttpRequestFactory requestFactory, QueryUtil queryUtil, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
      super(requestFactory, queryUtil, objectMapper, restConfigProperties);
    }

		@Override
    public AweSession getSession() { return testSession; }

    @Override
    public AweRequest getRequest() { return testRequest; }
  }
}
