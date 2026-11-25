package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.enums.TriggerType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Rules for overriding task parameter values at launch time: which parameter sources a launch may override, how
 * supplied values are applied, which parent values a dependency inherits and which property keys do not resolve.
 */
public class TaskParameterOverrides {

  /**
   * Sources overridable by every launch that is not manual: only VARIABLE parameters
   */
  public static final Set<String> VARIABLE_SOURCES = Set.of(String.valueOf(ParameterConstants.VARIABLE));

  /**
   * Sources overridable by a manual launch: the values and the variables. A property keeps its key: the launch that
   * changes it is rejected ({@link #changedPropertyKeys(Task, Map)}), so its configured value is never swapped for the
   * value of another key
   */
  private static final Set<String> MANUAL_SOURCES = Set.of(String.valueOf(ParameterConstants.VALUE),
    String.valueOf(ParameterConstants.VARIABLE));
  private static final String PROPERTY_SOURCE = String.valueOf(ParameterConstants.PROPERTY);

  private final UnaryOperator<String> propertyResolver;

  /**
   * Constructor
   *
   * @param propertyResolver Resolves a property key to its configured value (null when not defined)
   */
  public TaskParameterOverrides(UnaryOperator<String> propertyResolver) {
    this.propertyResolver = propertyResolver;
  }

  /**
   * Parameter sources a launch of the given type may override. An operator launching by hand may
   * adjust the VALUE and VARIABLE parameters (a PROPERTY keeps its key); every other trigger type,
   * dependency propagation included, may only override VARIABLE parameters.
   *
   * @param triggerType Trigger type of the launch
   * @return Overridable parameter sources
   */
  public Set<String> overridableSources(TriggerType triggerType) {
    return TriggerType.MANUAL.equals(triggerType) ? MANUAL_SOURCES : VARIABLE_SOURCES;
  }

  /**
   * Apply the supplied values to the task parameters whose source is overridable. A value of a type that is not text
   * goes without its surrounding blanks (as it is validated, {@link #invalidValues(Task, Map)}).
   *
   * @param task               Task holding the parameter list to update
   * @param variables          Supplied values (parameter name -&gt; value); null or empty is a no-op
   * @param overridableSources Parameter sources the supplied values may override
   */
  public void apply(Task task, Map<String, String> variables, Set<String> overridableSources) {
    if (variables == null || variables.isEmpty() || task.getParameterList() == null) {
      return;
    }

    for (TaskParameter parameter : task.getParameterList()) {
      if (overridableSources.contains(parameter.getSource()) && variables.containsKey(parameter.getName())) {
        parameter.setValue(ParameterValues.normalize(parameter.getType(), variables.get(parameter.getName())));
      }
    }
  }

  /**
   * Property keys of the task PROPERTY parameters that do not resolve against the configuration.
   *
   * @param task Task holding the parameters
   * @return Property keys that do not resolve (blank or null keys as empty text), in parameter order
   */
  public List<String> unresolvedProperties(Task task) {
    if (task.getParameterList() == null) {
      return Collections.emptyList();
    }

    return task.getParameterList().stream()
      .filter(parameter -> PROPERTY_SOURCE.equals(parameter.getSource()))
      .map(TaskParameter::getValue)
      .filter(key -> key == null || key.isBlank() || propertyResolver.apply(key) == null)
      .map(key -> key == null ? "" : key)
      .toList();
  }

  /**
   * Names of the parameters whose supplied value is not a value of their type, in parameter order. Only a value that
   * changes the stored one is checked (a launch keeps working with what the task already had), and never on a
   * {@link ParameterConstants#PROPERTY} parameter, whose value is a property key ({@link #changedPropertyKeys(Task, Map)}
   * and {@link #unresolvedProperties(Task)} check it). A value of a type that is not text is checked without its
   * surrounding blanks; a blank value is only valid for the text types (STRING, OBJECT): every other type needs a value.
   *
   * @param task      Task holding the stored parameters; null when it does not exist (nothing is checked: the launch
   *                  answers that the task does not exist)
   * @param variables Supplied values (parameter name -&gt; value); null or empty for none
   * @return Names of the parameters with a value that is not valid for their type
   */
  public List<String> invalidValues(Task task, Map<String, String> variables) {
    return suppliedParameters(task, variables)
      .filter(parameter -> !PROPERTY_SOURCE.equals(parameter.getSource()))
      .filter(parameter -> {
        String value = ParameterValues.normalize(parameter.getType(), variables.get(parameter.getName()));
        return !textOf(parameter.getValue()).equals(textOf(value)) && !ParameterValues.isValid(parameter.getType(), value);
      })
      .map(TaskParameter::getName)
      .toList();
  }

  /**
   * Names of the {@link ParameterConstants#PROPERTY} parameters whose supplied value is not their key (blanks around it
   * aside), in parameter order: a launch cannot change the key of a property, it would read another configured value.
   *
   * @param task      Task holding the stored parameters; null when it does not exist (nothing is checked)
   * @param variables Supplied values (parameter name -&gt; value); null or empty for none
   * @return Names of the property parameters whose key would change
   */
  public List<String> changedPropertyKeys(Task task, Map<String, String> variables) {
    return suppliedParameters(task, variables)
      .filter(parameter -> PROPERTY_SOURCE.equals(parameter.getSource()))
      .filter(parameter -> !textOf(parameter.getValue()).strip().equals(textOf(variables.get(parameter.getName())).strip()))
      .map(TaskParameter::getName)
      .toList();
  }

  /**
   * Parameters of the task with a supplied value, none without a task, parameters or values
   */
  private static Stream<TaskParameter> suppliedParameters(Task task, Map<String, String> variables) {
    if (task == null || variables == null || variables.isEmpty() || task.getParameterList() == null) {
      return Stream.empty();
    }
    return task.getParameterList().stream()
      .filter(parameter -> parameter.getName() != null && variables.containsKey(parameter.getName()));
  }

  private static String textOf(String value) {
    return value == null ? "" : value;
  }

  /**
   * Build a name -&gt; value map from a task's parameter list, to be propagated to its dependencies.
   * PROPERTY-source parameters store the property KEY rather than the value, so they are resolved here
   * and the RESOLVED value is propagated. Any other source propagates its stored value verbatim.
   * A parameter with a null name or a null value is treated as "not supplied" and skipped, so it never
   * overrides a child's stored default (the child keeps its own value as the fallback); a PROPERTY parameter
   * whose key resolves to null is likewise treated as "not supplied".
   *
   * @param task Task holding the parameters
   * @return Ordered map of parameter name to value; empty when the task has no parameters
   */
  public Map<String, String> propagatedValues(Task task) {
    Map<String, String> values = new LinkedHashMap<>();
    if (task.getParameterList() != null) {
      for (TaskParameter parameter : task.getParameterList()) {
        if (parameter.getName() != null && parameter.getValue() != null) {
          String value = parameter.getValue();
          // PROPERTY-source parameters hold the property KEY; resolve it to the actual value so the
          // child receives the resolved value instead of the raw key.
          if (PROPERTY_SOURCE.equals(parameter.getSource())) {
            value = propertyResolver.apply(parameter.getValue());
          }
          if (value != null) {
            values.put(parameter.getName(), value);
          }
        }
      }
    }
    return values;
  }
}
