package com.almis.awe.service.connector;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.RestConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.services.AbstractServiceRest;
import com.almis.awe.model.entities.services.ServiceInputParameter;
import com.almis.awe.model.entities.services.ServiceMicroservice;
import com.almis.awe.model.rest.RestParameter;
import com.almis.awe.model.rest.ServiceAuth;
import com.almis.awe.model.rest.ServiceDetails;
import com.almis.awe.model.type.ServiceAuthType;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

  // Non-list contract normalization: collection value for a non-list parameter uses the first element and warns
  @Test
  void normalizeNonListParameters_nonListParamWithCollectionValue_usesFirstElementAndWarns() {
    // Given
    ListAppender<ILoggingEvent> appender = attachLogAppender();
    try {
      ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("Als", false));
      Map<String, Object> params = new HashMap<>();
      params.put("Als", List.of("A1", "A2"));

      // When
      ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

      // Then
      assertEquals("A1", params.get("Als"));
      assertTrue(appender.list.stream().anyMatch(event -> Level.WARN.equals(event.getLevel())
          && event.getFormattedMessage().contains("Als")
          && event.getFormattedMessage().contains("alu-service")),
        "A WARN log with parameter and service names was expected");
    } finally {
      detachLogAppender(appender);
    }
  }

  // Non-list contract normalization: empty collection value for a non-list parameter becomes an empty string
  @Test
  void normalizeNonListParameters_nonListParamWithEmptyCollection_usesEmptyString() {
    // Given
    ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("Als", false));
    Map<String, Object> params = new HashMap<>();
    params.put("Als", List.of());

    // When
    ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

    // Then
    assertEquals("", params.get("Als"));
  }

  // Non-list contract normalization: array node value for a non-list parameter uses the first node
  @Test
  void normalizeNonListParameters_nonListParamWithArrayNode_usesFirstNode() {
    // Given
    ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("Prd", false));
    Map<String, Object> params = new HashMap<>();
    ArrayNode arrayValue = JsonNodeFactory.instance.arrayNode().add("P1").add("P2");
    params.put("Prd", arrayValue);

    // When
    ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

    // Then
    assertEquals(JsonNodeFactory.instance.textNode("P1"), params.get("Prd"));
  }

  // Non-list contract normalization: empty array node value for a non-list parameter becomes an empty text node
  @Test
  void normalizeNonListParameters_nonListParamWithEmptyArrayNode_usesEmptyTextNode() {
    // Given
    ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("Prd", false));
    Map<String, Object> params = new HashMap<>();
    params.put("Prd", JsonNodeFactory.instance.arrayNode());

    // When
    ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

    // Then
    assertEquals(JsonNodeFactory.instance.textNode(""), params.get("Prd"));
  }

  // Non-list contract normalization: list parameters keep their collection value untouched
  @Test
  void normalizeNonListParameters_listParamWithCollectionValue_isLeftUntouched() {
    // Given
    ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("AlsList", true));
    Map<String, Object> params = new HashMap<>();
    List<String> listValue = List.of("A1", "A2");
    params.put("AlsList", listValue);

    // When
    ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

    // Then
    assertSame(listValue, params.get("AlsList"));
  }

  // Non-list contract normalization: scalar values stay untouched and produce no warning
  @Test
  void normalizeNonListParameters_nonListParamWithScalarValue_isLeftUntouched() {
    // Given
    ListAppender<ILoggingEvent> appender = attachLogAppender();
    try {
      ServiceMicroservice microservice = buildMicroservice("alu-service", buildServiceParameter("Als", false));
      Map<String, Object> params = new HashMap<>();
      params.put("Als", "scalar-value");

      // When
      ReflectionTestUtils.invokeMethod(connector, "normalizeNonListParameters", microservice, params);

      // Then
      assertEquals("scalar-value", params.get("Als"));
      assertTrue(appender.list.stream().noneMatch(event -> Level.WARN.equals(event.getLevel())),
        "No WARN log was expected for a scalar value");
    } finally {
      detachLogAppender(appender);
    }
  }

  // Issue #850: launching a microservice must not mutate the shared service definition
  @Test
  void launch_repeatedCalls_doNotMutateSharedService() throws Exception {
    // Given
    ServiceMicroservice shared = buildMicroservice("alu-service", buildServiceParameter("Als", false));
    shared.setEndpoint("/search");
    stubServiceConfig("alu-service", authDetails(), staticRestParameter("tenant", "acme"));

    // When
    for (int i = 0; i < 5; i++) {
      connector.launch(shared, new HashMap<>());
    }

    // Then
    assertEquals(1, shared.getParameterList().size(), "The shared parameter list must keep its size");
    assertEquals("Als", shared.getParameterList().get(0).getName());
    assertNull(shared.getAuthentication(), "The shared service authentication must stay untouched");
    assertNull(shared.getUsername(), "The shared service username must stay untouched");
    assertNull(shared.getPassword(), "The shared service password must stay untouched");
  }

  // Issue #850: the request still receives the XML parameters plus the configured ones, exactly once
  @Test
  void launch_requestSeesXmlAndConfiguredParametersExactlyOnce() throws Exception {
    // Given
    ServiceMicroservice shared = buildMicroservice("alu-service", buildServiceParameter("Als", false));
    shared.setEndpoint("/search");
    stubServiceConfig("alu-service", authDetails(), staticRestParameter("tenant", "acme"));

    // When
    connector.launch(shared, new HashMap<>());
    connector.launch(shared, new HashMap<>());

    // Then
    assertEquals(2, connector.requestedParameterNames.size());
    connector.requestedParameterNames.forEach(names -> assertEquals(List.of("Als", "tenant"), names));
    assertEquals(ServiceAuthType.BASIC, connector.requestedAuthentication.get(0));
    assertEquals("user", connector.requestedUsernames.get(0));
  }

  // Issue #850: concurrent calls on the same shared service must not fail (smoke test, race dependent)
  @Test
  void launch_concurrentCallsOnSharedService_doNotFailNorGrowTheList() throws Exception {
    // Given
    int tasks = 20;
    ServiceMicroservice shared = buildMicroservice("alu-service", buildServiceParameter("Als", false));
    shared.setEndpoint("/search");
    stubServiceConfig("alu-service", authDetails(), staticRestParameter("tenant", "acme"));
    connector.iterateParametersOnRequest = true;
    ExecutorService executor = Executors.newFixedThreadPool(tasks);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Void>> futures = new ArrayList<>();

    try {
      // When
      for (int i = 0; i < tasks; i++) {
        futures.add(executor.submit(() -> {
          assertTrue(start.await(10, TimeUnit.SECONDS));
          for (int j = 0; j < 20; j++) {
            connector.launch(shared, new HashMap<>());
          }
          return null;
        }));
      }
      start.countDown();

      // Then
      for (Future<Void> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
      assertEquals(1, shared.getParameterList().size(), "The shared parameter list must keep its size");
      assertEquals(tasks * 20, connector.requestedParameterNames.size());
      connector.requestedParameterNames.forEach(names -> assertEquals(List.of("Als", "tenant"), names));
    } finally {
      executor.shutdownNow();
    }
  }

  private void stubServiceConfig(String name, ServiceDetails details, RestParameter... parameters) {
    details.setName(name);
    details.setParameters(new ArrayList<>(Arrays.asList(parameters)));
    when(restConfigProperties.getServices()).thenReturn(Map.of(name, details));
  }

  private ServiceDetails authDetails() {
    ServiceAuth auth = new ServiceAuth();
    auth.setUsername("user");
    auth.setPassword("secret");
    return new ServiceDetails().setBaseUrl("http://localhost:8080").setAuthentication(auth);
  }

  private RestParameter staticRestParameter(String name, String value) {
    RestParameter parameter = new RestParameter();
    parameter.setName(name);
    parameter.setValue(value);
    parameter.setType(RestParameter.ServiceParameterType.VALUE);
    return parameter;
  }

  private ServiceMicroservice buildMicroservice(String name, ServiceInputParameter... parameters) {
    ServiceMicroservice microservice = new ServiceMicroservice();
    microservice.setName(name);
    microservice.setParameterList(new ArrayList<>(Arrays.asList(parameters)));
    return microservice;
  }

  private ServiceInputParameter buildServiceParameter(String name, boolean list) {
    ServiceInputParameter parameter = new ServiceInputParameter();
    parameter.setName(name);
    parameter.setList(list);
    return parameter;
  }

  private ListAppender<ILoggingEvent> attachLogAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(MicroserviceConnector.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  private void detachLogAppender(ListAppender<ILoggingEvent> appender) {
    Logger logger = (Logger) LoggerFactory.getLogger(MicroserviceConnector.class);
    logger.detachAppender(appender);
    appender.stop();
  }

  // Test helper subclass to inject session/request without Spring context
  @Setter
	static class TestableMicroserviceConnector extends MicroserviceConnector {
    private AweSession testSession;
    private AweRequest testRequest;
    private boolean iterateParametersOnRequest;
    private final List<List<String>> requestedParameterNames = new CopyOnWriteArrayList<>();
    private final List<Object> requestedAuthentication = new CopyOnWriteArrayList<>();
    private final List<String> requestedUsernames = new CopyOnWriteArrayList<>();

    public TestableMicroserviceConnector(ClientHttpRequestFactory requestFactory, QueryUtil queryUtil, ObjectMapper objectMapper, RestConfigProperties restConfigProperties) {
      super(requestFactory, queryUtil, objectMapper, restConfigProperties);
    }

		@Override
    public AweSession getSession() { return testSession; }

    @Override
    public AweRequest getRequest() { return testRequest; }

    @Override
    protected ServiceData doRequest(String url, AbstractServiceRest service, Map<String, Object> paramsMapFromRequest) throws AWException {
      List<String> names = new ArrayList<>();
      for (ServiceInputParameter parameter : service.getParameterList()) {
        names.add(parameter.getName());
        if (iterateParametersOnRequest) {
          Thread.yield();
        }
      }
      requestedParameterNames.add(names);
      requestedAuthentication.add(service.getAuthentication());
      requestedUsernames.add(service.getUsername());
      return new ServiceData();
    }
  }
}
