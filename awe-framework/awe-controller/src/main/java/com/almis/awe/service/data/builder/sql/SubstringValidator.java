package com.almis.awe.service.data.builder.sql;

import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Variable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Validates the operands of a SUBSTRING operation. It reports the first violation instead of throwing, so the caller
 * builds the localized exception. Used by {@code SQLBuilder}; it is not an extension point.
 * <p>
 * Accepted operand counts:
 * <ul>
 *   <li>2 operands — {@code SUBSTRING(source, beginIndex)}: extracts from {@code beginIndex} to end of string.</li>
 *   <li>3 operands — {@code SUBSTRING(source, beginIndex, endIndex)}: extracts {@code [beginIndex, endIndex)}.</li>
 * </ul>
 * Index operands must be integer-compatible: a {@link Constant} of type {@code INTEGER} or {@code LONG}, or a
 * {@link Field}. A field that references a variable must reference an {@code INTEGER} or {@code LONG} variable; a
 * field without variable (a column) or whose variable is not defined (a runtime parameter) is allowed through.
 */
public final class SubstringValidator {

  /**
   * Locale key of an invalid operand count; its argument is the count
   */
  public static final String OPERAND_COUNT = "ERROR_MESSAGE_SUBSTRING_OPERAND_COUNT";

  /**
   * Locale key of an invalid index operand; its arguments are the position (2 or 3) and the operand type
   */
  public static final String OPERAND_TYPE = "ERROR_MESSAGE_SUBSTRING_OPERAND_TYPE";

  private static final String TYPE_INTEGER = "INTEGER";
  private static final String TYPE_LONG = "LONG";

  private SubstringValidator() {
  }

  /**
   * A SUBSTRING operation is not valid
   *
   * @param messageKey Locale key of the message
   * @param arguments  Message arguments
   */
  public record Violation(String messageKey, List<String> arguments) {
  }

  /**
   * Validate the operands of a SUBSTRING operation
   *
   * @param operands            Operands, the source first
   * @param variableDefinitions Retrieves the definition of a query variable, or null when it is not defined. It is
   *                            only called for index fields that reference a variable
   * @return First violation, or empty when the operation is valid
   */
  public static Optional<Violation> validate(List<SqlField> operands, Function<String, Variable> variableDefinitions) {
    int count = operands.size();
    if (count != 2 && count != 3) {
      return Optional.of(new Violation(OPERAND_COUNT, List.of(String.valueOf(count))));
    }
    Optional<Violation> violation = validateIndex(operands.get(1), 2, variableDefinitions);
    if (violation.isEmpty() && count == 3) {
      violation = validateIndex(operands.get(2), 3, variableDefinitions);
    }
    return violation;
  }

  private static Optional<Violation> validateIndex(SqlField operand, int position,
                                                   Function<String, Variable> variableDefinitions) {
    String type;
    if (operand instanceof Constant constant) {
      type = constant.getType() == null ? "STRING (no type)" : constant.getType();
    } else if (operand instanceof Field field) {
      Variable variable = field.getVariable() == null ? null : variableDefinitions.apply(field.getVariable());
      if (variable == null) {
        return Optional.empty();
      }
      type = variable.getType();
    } else {
      return Optional.of(typeViolation(position, operand == null ? "null" : operand.getClass().getSimpleName()));
    }
    return TYPE_INTEGER.equals(type) || TYPE_LONG.equals(type) ? Optional.empty()
      : Optional.of(typeViolation(position, type));
  }

  private static Violation typeViolation(int position, String type) {
    // The type of a variable may be null, so the arguments are not an immutable List.of
    return new Violation(OPERAND_TYPE, Arrays.asList(String.valueOf(position), type));
  }
}
