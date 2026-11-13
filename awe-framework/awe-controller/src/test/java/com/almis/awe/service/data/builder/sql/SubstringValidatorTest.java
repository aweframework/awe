package com.almis.awe.service.data.builder.sql;

import com.almis.awe.model.entities.queries.Constant;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Operation;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Unit tests for {@link SubstringValidator}
 */
class SubstringValidatorTest {

  private static final Field SOURCE = Field.builder().id("col").table("t").build();
  private static final Map<String, Variable> VARIABLES = Map.of(
    "int", Variable.builder().id("int").type("INTEGER").build(),
    "long", Variable.builder().id("long").type("LONG").build(),
    "text", Variable.builder().id("text").type("STRING").build(),
    "untyped", Variable.builder().id("untyped").build());
  private static final Function<String, Variable> DEFINITIONS = VARIABLES::get;

  @Test
  @DisplayName("Two or three integer-compatible operands are valid")
  void validOperands() {
    assertThat(SubstringValidator.validate(List.of(SOURCE, constant("INTEGER")), DEFINITIONS)).isEmpty();
    assertThat(SubstringValidator.validate(List.of(SOURCE, constant("LONG"), variable("int")), DEFINITIONS))
      .isEmpty();
    assertThat(SubstringValidator.validate(List.of(SOURCE, variable("long")), DEFINITIONS)).isEmpty();
  }

  @Test
  @DisplayName("Columns and undefined variables are allowed through")
  void columnsAndUndefinedVariablesAreAllowed() {
    Field column = Field.builder().id("start").table("t").build();

    assertThat(SubstringValidator.validate(List.of(SOURCE, column),
      name -> fail("a column does not look up variables"))).isEmpty();
    assertThat(SubstringValidator.validate(List.of(SOURCE, variable("runtime")), DEFINITIONS)).isEmpty();
  }

  @Test
  @DisplayName("Any other operand count is a count violation")
  void invalidCount() {
    assertThat(SubstringValidator.validate(List.of(SOURCE), DEFINITIONS)).contains(
      new SubstringValidator.Violation(SubstringValidator.OPERAND_COUNT, List.of("1")));
    List<SqlField> four = List.of(SOURCE, constant("INTEGER"), constant("INTEGER"), constant("INTEGER"));
    assertThat(SubstringValidator.validate(four, DEFINITIONS)).contains(
      new SubstringValidator.Violation(SubstringValidator.OPERAND_COUNT, List.of("4")));
  }

  @Test
  @DisplayName("A non integer operand is a type violation naming its position and type")
  void invalidType() {
    Operation operation = new Operation();

    assertThat(SubstringValidator.validate(List.of(SOURCE, new Constant().setValue("1")), DEFINITIONS))
      .contains(typeViolation("2", "STRING (no type)"));
    assertThat(SubstringValidator.validate(List.of(SOURCE, constant("INTEGER"), constant("FLOAT")), DEFINITIONS))
      .contains(typeViolation("3", "FLOAT"));
    assertThat(SubstringValidator.validate(List.of(SOURCE, variable("text")), DEFINITIONS))
      .contains(typeViolation("2", "STRING"));
    assertThat(SubstringValidator.validate(List.of(SOURCE, variable("untyped")), DEFINITIONS))
      .contains(typeViolation("2", null));
    assertThat(SubstringValidator.validate(List.of(SOURCE, operation), DEFINITIONS))
      .contains(typeViolation("2", "Operation"));
    assertThat(SubstringValidator.validate(Arrays.asList(SOURCE, null), DEFINITIONS))
      .contains(typeViolation("2", "null"));
  }

  @Test
  @DisplayName("The first violation is reported")
  void firstViolationIsReported() {
    assertThat(SubstringValidator.validate(List.of(SOURCE, constant("FLOAT"), constant("DOUBLE")), DEFINITIONS))
      .contains(typeViolation("2", "FLOAT"));
  }

  private static SubstringValidator.Violation typeViolation(String position, String type) {
    return new SubstringValidator.Violation(SubstringValidator.OPERAND_TYPE, Arrays.asList(position, type));
  }

  private static Constant constant(String type) {
    return (Constant) new Constant().setType(type).setValue("1");
  }

  private static Field variable(String name) {
    return (Field) new Field().setVariable(name);
  }
}
