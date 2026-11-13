package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MicrometerExecutionObserverTest {

  private final TestObservationRegistry registry = TestObservationRegistry.create();
  private final MicrometerExecutionObserver observer = new MicrometerExecutionObserver(() -> registry);

  @Test
  @DisplayName("A query is an awe.query observation with its id, the tags it brings and a success outcome")
  void observesQueries() throws AWException {
    ServiceData result = new ServiceData();

    ServiceData returned = observer.observe(ExecutionType.QUERY, "Q1", Map.of("connector", "sql"), () -> result);

    assertThat(returned).isSameAs(result);
    assertThat(registry).hasSingleObservationThat()
      .hasNameEqualTo("awe.query")
      .hasLowCardinalityKeyValue("id", "Q1")
      .hasLowCardinalityKeyValue("connector", "sql")
      .hasLowCardinalityKeyValue("outcome", "success")
      .hasBeenStopped();
  }

  @Test
  @DisplayName("Maintains and actions are awe.maintain and awe.action observations")
  void observesMaintainsAndActions() throws AWException {
    observer.observe(ExecutionType.MAINTAIN, "M1", Map.of(), () -> "done");
    observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> "done");

    assertThat(registry).hasNumberOfObservationsEqualTo(2)
      .hasObservationWithNameEqualTo("awe.maintain").that().hasLowCardinalityKeyValue("id", "M1");
    assertThat(registry).hasObservationWithNameEqualTo("awe.action").that().hasLowCardinalityKeyValue("id", "A1");
  }

  @Test
  @DisplayName("A failing execution records the error, rethrows the same exception and is an error outcome")
  void failingExecution() {
    AWException failure = new AWException("title", "message");

    assertThatThrownBy(() -> observer.observe(ExecutionType.QUERY, "Q1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);

    assertThat(registry).hasSingleObservationThat()
      .hasLowCardinalityKeyValue("outcome", "error")
      .assertThatError().isSameAs(failure);
  }

  @Test
  @DisplayName("A runtime exception is recorded and rethrown too")
  void runtimeException() {
    IllegalStateException failure = new IllegalStateException("boom");

    assertThatThrownBy(() -> observer.observe(ExecutionType.MAINTAIN, "M1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);

    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "error");
  }

  @Test
  @DisplayName("An answer of error type is an error outcome even though nothing was thrown")
  void errorAnswer() throws AWException {
    observer.observe(ExecutionType.QUERY, "Q1", Map.of(), () -> new ServiceData().setType(AnswerType.ERROR));

    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "error");
  }

  @Test
  @DisplayName("Warnings and infos are still a success")
  void warningIsSuccess() throws AWException {
    observer.observe(ExecutionType.QUERY, "Q1", Map.of(), () -> new ServiceData().setType(AnswerType.WARNING));

    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "success");
  }

  @Test
  @DisplayName("Without an id the observation says undefined, never null")
  void missingId() throws AWException {
    observer.observe(ExecutionType.ACTION, null, Map.of(), () -> "done");

    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("id", "undefined");
  }

  @Test
  @DisplayName("The execution runs untouched when there is no observation registry")
  void noRegistry() throws AWException {
    MicrometerExecutionObserver withoutRegistry = new MicrometerExecutionObserver(() -> null);

    assertThat(withoutRegistry.observe(ExecutionType.QUERY, "Q1", Map.of(), () -> "done")).isEqualTo("done");
  }

  @Test
  @DisplayName("An AWException of warning or info type is a business message: success outcome, rethrown, no error recorded")
  void businessMessageIsSuccess() {
    for (AnswerType type : new AnswerType[]{AnswerType.WARNING, AnswerType.INFO}) {
      AWException message = new AWException("title", "message", type);
      registry.clear();

      assertThatThrownBy(() -> observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> {
        throw message;
      })).isSameAs(message);

      assertThat(registry).hasSingleObservationThat()
        .hasLowCardinalityKeyValue("outcome", "success")
        .doesNotHaveError();
    }
  }

  @Test
  @DisplayName("An AWException of error type is a real failure: error outcome and the error recorded")
  void errorExceptionIsFailure() {
    AWException failure = new AWException("title", "message", AnswerType.ERROR);

    assertThatThrownBy(() -> observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);

    assertThat(registry).hasSingleObservationThat()
      .hasLowCardinalityKeyValue("outcome", "error")
      .assertThatError().isSameAs(failure);
  }

  @Test
  @DisplayName("The answer type the execution reports decides the outcome of a result that is not ServiceData, such as an action")
  void answerTypeDecidesTheOutcome() throws AWException {
    observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> List.of(), result -> AnswerType.ERROR);
    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "error").doesNotHaveError();

    for (AnswerType type : new AnswerType[]{AnswerType.OK, AnswerType.WARNING, AnswerType.INFO, null}) {
      registry.clear();
      observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> List.of(), result -> type);
      assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "success");
    }
  }

  @Test
  @DisplayName("The answer type wins over the data type of the result")
  void answerTypeWinsOverData() throws AWException {
    observer.observe(ExecutionType.QUERY, "Q1", Map.of(), () -> new ServiceData().setType(AnswerType.ERROR), result -> AnswerType.OK);

    assertThat(registry).hasSingleObservationThat().hasLowCardinalityKeyValue("outcome", "success");
  }
}
