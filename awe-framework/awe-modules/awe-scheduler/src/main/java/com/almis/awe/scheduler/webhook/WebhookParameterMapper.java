package com.almis.awe.scheduler.webhook;

import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Takes from the data of a webhook the values of the task parameters. The parameters with a JSON pointer are the
 * allow list: only the variable parameters that have one receive a value, and only when the data holds it.
 */
public final class WebhookParameterMapper {

  private static final String VARIABLE_SOURCE = String.valueOf(ParameterConstants.VARIABLE);

  private WebhookParameterMapper() {
  }

  /**
   * Map the data to variables
   *
   * @param parameters Task parameters
   * @param data       Data of the webhook
   * @return Value (as text) of every mapped variable parameter that is present in the data
   */
  public static Map<String, String> variables(List<TaskParameter> parameters, JsonNode data) {
    Map<String, String> variables = new HashMap<>();
    if (parameters == null) {
      return variables;
    }
    for (TaskParameter parameter : parameters) {
      String pointer = parameter.getPointer();
      if (VARIABLE_SOURCE.equals(parameter.getSource()) && pointer != null && !pointer.isBlank()) {
        valueOf(data, pointer.trim()).ifPresent(value -> variables.put(parameter.getName(), value));
      }
    }
    return variables;
  }

  private static Optional<String> valueOf(JsonNode data, String pointer) {
    try {
      JsonNode node = data.at(pointer);
      if (node.isMissingNode() || node.isNull()) {
        return Optional.empty();
      }
      return Optional.of(node.isString() ? node.asString() : node.toString());
    } catch (IllegalArgumentException exc) {
      // A pointer that is not valid (it must start with a slash) gives no value
      return Optional.empty();
    }
  }
}
