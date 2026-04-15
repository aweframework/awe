package com.almis.awe.service.connector;

import com.almis.awe.model.entities.services.ServiceInputParameter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractServiceConnectorTest {

  private final TestConnector connector = new TestConnector();

  @Test
  void shouldCoerceIntegerStringsToInteger() {
    ServiceInputParameter parameter = buildParameter("INTEGER");

    assertEquals(1, connector.getParameterValue(parameter, "1"));
    assertEquals(1, connector.getParameterValue(parameter, 1));
    assertNull(connector.getParameterValue(parameter, ""));
  }

  @Test
  void shouldCoerceLongStringsToLong() {
    ServiceInputParameter parameter = buildParameter("LONG");

    assertEquals(1L, connector.getParameterValue(parameter, "1"));
    assertEquals(1L, connector.getParameterValue(parameter, 1L));
    assertNull(connector.getParameterValue(parameter, ""));
  }

  @Test
  void shouldCoerceFloatAndDoubleStrings() {
    ServiceInputParameter floatParameter = buildParameter("FLOAT");
    ServiceInputParameter doubleParameter = buildParameter("DOUBLE");

    assertEquals(1.5f, (Float) connector.getParameterValue(floatParameter, "1.5"), 0.0001f);
    assertEquals(1.5d, (Double) connector.getParameterValue(doubleParameter, "1.5"), 0.0001d);
    assertNull(connector.getParameterValue(floatParameter, ""));
    assertNull(connector.getParameterValue(doubleParameter, ""));
  }

  @Test
  void shouldCoerceBooleanStrings() {
    ServiceInputParameter parameter = buildParameter("BOOLEAN");

    assertTrue((Boolean) connector.getParameterValue(parameter, "true"));
    assertFalse((Boolean) connector.getParameterValue(parameter, "false"));
    assertTrue((Boolean) connector.getParameterValue(parameter, true));
    assertNull(connector.getParameterValue(parameter, ""));
  }

  private ServiceInputParameter buildParameter(String type) {
    ServiceInputParameter parameter = new ServiceInputParameter();
    parameter.setType(type);
    parameter.setName("testParameter");
    return parameter;
  }

  private static class TestConnector extends AbstractServiceConnector {

    TestConnector() {
      super(new ObjectMapper());
    }

    @Override
    public com.almis.awe.model.dto.ServiceData launch(com.almis.awe.model.entities.services.ServiceType service, Map<String, Object> paramsMapFromRequest) {
      return null;
    }

    @Override
    public com.almis.awe.model.dto.ServiceData subscribe(com.almis.awe.model.entities.queries.Query query) {
      return null;
    }
  }
}
