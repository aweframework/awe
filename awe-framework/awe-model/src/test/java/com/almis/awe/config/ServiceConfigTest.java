package com.almis.awe.config;

import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceConfigTest {

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private PrototypeRequestBeanHolder prototypeRequestBeanHolder;

  private TestServiceConfig serviceConfig;

  @BeforeEach
  void setUp() {
    serviceConfig = new TestServiceConfig();
    serviceConfig.setApplicationContext(applicationContext);
  }

  @Test
  void getRequestParametersDelegatesToQueryUtil() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("foo", "bar");
    when(applicationContext.getBean(QueryUtil.class)).thenReturn(queryUtil);
    when(queryUtil.getParameters()).thenReturn(parameters);

    assertEquals(parameters, serviceConfig.getRequestParameters());
  }

  @Test
  void getRequestParameterAsStringReturnsNullForMissingParameter() {
    when(applicationContext.getBean(QueryUtil.class)).thenReturn(queryUtil);
    when(queryUtil.getRequestParameter("missing")).thenReturn(null);

    assertNull(serviceConfig.getRequestParameterAsString("missing"));
  }

  @Test
  void getRequestParameterAsStringReturnsTextValue() {
    when(applicationContext.getBean(QueryUtil.class)).thenReturn(queryUtil);
    when(queryUtil.getRequestParameter("user")).thenReturn(JsonNodeFactory.instance.textNode("snapshot-user"));

    assertEquals("snapshot-user", serviceConfig.getRequestParameterAsString("user"));
  }

  @Test
  void putRequestParameterWritesStringValueIntoExplicitSnapshot() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();

    serviceConfig.putRequestParameter(parameters, "PdfNam", "/tmp/report.pdf");

    assertEquals("/tmp/report.pdf", parameters.get("PdfNam").asText());
  }

  @Test
  void putRequestParameterWritesJsonValueIntoExplicitSnapshot() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    ObjectNode nestedValue = JsonNodeFactory.instance.objectNode().put("mode", "async");

    serviceConfig.putRequestParameter(parameters, "report", nestedValue);

    assertEquals("async", parameters.get("report").get("mode").asText());
  }

  @Test
  void putPropagatedRequestParameterDelegatesStringValueToHolder() {
    when(applicationContext.getBean(PrototypeRequestBeanHolder.class)).thenReturn(prototypeRequestBeanHolder);
    ArgumentCaptor<ObjectNode> parametersCaptor = ArgumentCaptor.forClass(ObjectNode.class);

    serviceConfig.putPropagatedRequestParameter("PdfNam", "/tmp/report.pdf");

    verify(prototypeRequestBeanHolder).mergeRequestData(parametersCaptor.capture());
    assertEquals("/tmp/report.pdf", parametersCaptor.getValue().get("PdfNam").asText());
  }

  @Test
  void putPropagatedRequestParameterDelegatesJsonValueToHolder() {
    when(applicationContext.getBean(PrototypeRequestBeanHolder.class)).thenReturn(prototypeRequestBeanHolder);
    ArgumentCaptor<ObjectNode> parametersCaptor = ArgumentCaptor.forClass(ObjectNode.class);
    ObjectNode nestedValue = JsonNodeFactory.instance.objectNode().put("mode", "async");

    serviceConfig.putPropagatedRequestParameter("report", nestedValue);

    verify(prototypeRequestBeanHolder).mergeRequestData(parametersCaptor.capture());
    assertEquals("async", parametersCaptor.getValue().get("report").get("mode").asText());
  }

  @Test
  void mergePropagatedRequestParametersDelegatesToHolder() {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("PdfNam", "/tmp/report.pdf");
    when(applicationContext.getBean(PrototypeRequestBeanHolder.class)).thenReturn(prototypeRequestBeanHolder);

    serviceConfig.mergePropagatedRequestParameters(parameters);

    verify(prototypeRequestBeanHolder).mergeRequestData(parameters);
  }

  private static class TestServiceConfig extends ServiceConfig {
  }
}
