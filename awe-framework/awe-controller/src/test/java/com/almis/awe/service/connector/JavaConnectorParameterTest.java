package com.almis.awe.service.connector;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.services.ServiceInputParameter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The values a Java service receives for its parameters: a screen sends the selected values of a grid column as a
 * list, and another screen may send it with the name of a single-value parameter (screen navigation sends the values of
 * the previous screen)
 */
class JavaConnectorParameterTest {

  private final JavaConnector connector = new JavaConnector(new JsonMapper());

  @Test
  void aSingleValueParameterTakesTheOnlyValueOfAList() throws AWException {
    assertThat(extract("INTEGER", List.of(7))).containsExactly(7);
    assertThat(extract("STRING", List.of())).containsExactly((Object) null);
    // A list of several values, or a parameter that accepts the list, is kept as it comes
    assertThat(extract("INTEGER", List.of(7, 8))).containsExactly(List.of(7, 8));
    assertThat(extract("OBJECT", List.of(7))).containsExactly(List.of(7));
  }

  private Object[] extract(String type, Object value) throws AWException {
    ServiceInputParameter parameter = new ServiceInputParameter();
    parameter.setName("user");
    parameter.setType(type);
    Map<String, Object> request = new HashMap<>();
    request.put("user", value);
    Object[] values = new Object[1];
    connector.extractParameters(List.of(parameter), request, values, new Class[1]);
    return Arrays.copyOf(values, 1);
  }
}
