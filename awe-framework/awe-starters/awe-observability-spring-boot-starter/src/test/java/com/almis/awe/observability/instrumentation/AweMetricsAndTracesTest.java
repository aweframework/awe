package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.observation.ExecutionObserver;
import com.almis.awe.service.observation.ExecutionObserver.ExecutionType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The observations the starter creates become metrics and spans through the handlers Spring Boot registers
 * (here wired by hand with a simple meter registry and a simple tracer).
 */
class AweMetricsAndTracesTest {

  private MeterRegistry meterRegistry;
  private SimpleTracer tracer;
  private ObservationRegistry registry;
  private ExecutionObserver observer;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    tracer = new SimpleTracer();
    registry = ObservationRegistry.create();
    registry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
    registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
    observer = new MicrometerExecutionObserver(() -> registry);
  }

  @Test
  @DisplayName("Screens and exports have their own timers, tagged with the option, screen, format and outcome")
  void screensAndExports() throws AWException {
    observer.observe(ExecutionType.SCREEN, "opt1", Map.of("screen", "scr1"), () -> "built");
    observer.observe(ExecutionType.SCREEN, "opt1", Map.of("screen", "scr1"), () -> "built");
    observer.observe(ExecutionType.EXPORT, "scr1", Map.of("format", "pdf"), () -> "file");

    assertThat(meterRegistry.get("awe.screen").tags("id", "opt1", "screen", "scr1", "outcome", "success").timer().count())
      .isEqualTo(2);
    assertThat(meterRegistry.get("awe.export").tags("id", "scr1", "format", "pdf", "outcome", "success").timer().count())
      .isEqualTo(1);
  }

  @Test
  @DisplayName("A disabled family runs its executions plainly: same result, no meter, no span; the other families are still observed")
  void disabledFamilies() throws AWException {
    ExecutionObserver limited = new MicrometerExecutionObserver(() -> registry, Set.of(ExecutionType.SCREEN, ExecutionType.EXPORT));

    assertThat(limited.observe(ExecutionType.SCREEN, "opt1", Map.of("screen", "scr1"), () -> "built")).isEqualTo("built");
    assertThat(limited.observe(ExecutionType.EXPORT, "scr1", Map.of("format", "pdf"), () -> "file", result -> AnswerType.OK))
      .isEqualTo("file");
    limited.observe(ExecutionType.QUERY, "Q1", Map.of(), ServiceData::new);

    assertThat(meterRegistry.find("awe.screen").meters()).isEmpty();
    assertThat(meterRegistry.find("awe.screen.active").meters()).isEmpty();
    assertThat(meterRegistry.find("awe.export").meters()).isEmpty();
    assertThat(meterRegistry.get("awe.query").tags("id", "Q1", "outcome", "success").timer().count()).isEqualTo(1);
    assertThat(tracer.getSpans()).extracting(SimpleSpan::getName).containsExactly("awe.query");
  }

  @Test
  @DisplayName("A disabled family still rethrows the failure of its execution")
  void disabledFamilyRethrows() {
    ExecutionObserver limited = new MicrometerExecutionObserver(() -> registry, Set.of(ExecutionType.SCREEN));
    AWException failure = new AWException("boom", "failed");

    assertThatThrownBy(() -> limited.observe(ExecutionType.SCREEN, "opt1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);
    assertThat(meterRegistry.find("awe.screen").meters()).isEmpty();
  }

  @Test
  @DisplayName("Every execution is a timer tagged with the id and the outcome, whose count is the number of executions")
  void metrics() throws AWException {
    observer.observe(ExecutionType.QUERY, "Q1", Map.of("connector", "sql"), ServiceData::new);
    observer.observe(ExecutionType.QUERY, "Q1", Map.of("connector", "sql"), ServiceData::new);
    assertThatThrownBy(() -> observer.observe(ExecutionType.QUERY, "Q1", Map.of("connector", "sql"), () -> {
      throw new AWException("boom", "failed");
    })).isInstanceOf(AWException.class);

    Timer success = meterRegistry.get("awe.query").tags("id", "Q1", "connector", "sql", "outcome", "success").timer();
    Timer failure = meterRegistry.get("awe.query").tags("id", "Q1", "outcome", "error", "error", "AWException").timer();
    assertThat(success.count()).isEqualTo(2);
    assertThat(failure.count()).isEqualTo(1);
    assertThat(success.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS)).isPositive();
  }

  @Test
  @DisplayName("Maintains and actions have their own timers")
  void otherTypes() throws AWException {
    observer.observe(ExecutionType.MAINTAIN, "M1", Map.of(), () -> "done");
    observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> "done");

    assertThat(meterRegistry.get("awe.maintain").tags("id", "M1", "outcome", "success").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.get("awe.action").tags("id", "A1", "outcome", "success").timer().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Every execution is a span with the id, and it is the current span while the execution runs")
  void traces() throws AWException {
    AtomicReference<Span> current = new AtomicReference<>();

    observer.observe(ExecutionType.QUERY, "Q1", Map.of("connector", "sql"), () -> {
      current.set(tracer.currentSpan());
      return new ServiceData();
    });

    assertThat(tracer.getSpans()).hasSize(1);
    SimpleSpan span = tracer.getSpans().peek();
    assertThat(span.getName()).isEqualTo("awe.query");
    assertThat(span.getTags()).containsEntry("id", "Q1").containsEntry("connector", "sql").containsEntry("outcome", "success");
    assertThat(current.get()).isNotNull();
    assertThat(current.get().context().traceId()).isEqualTo(span.context().traceId());
    assertThat(span.getEndTimestamp()).isNotNull();
  }

  @Test
  @DisplayName("A failing execution marks its span with the error")
  void failingSpan() {
    AWException failure = new AWException("boom", "failed");

    assertThatThrownBy(() -> observer.observe(ExecutionType.MAINTAIN, "M1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);

    SimpleSpan span = tracer.getSpans().peek();
    assertThat(span.getError()).isSameAs(failure);
    assertThat(span.getTags()).containsEntry("outcome", "error");
  }

  @Test
  @DisplayName("An execution inside an existing span becomes a child of it, so the trace id is kept")
  void childOfTheCurrentSpan() throws AWException {
    Span parent = tracer.nextSpan().name("http request").start();

    try (io.micrometer.tracing.Tracer.SpanInScope ignored = tracer.withSpan(parent)) {
      observer.observe(ExecutionType.QUERY, "Q1", Map.of(), ServiceData::new);
    }
    parent.end();

    SimpleSpan child = tracer.getSpans().stream().filter(span -> "awe.query".equals(span.getName())).findFirst().orElseThrow();
    assertThat(child.context().traceId()).isEqualTo(parent.context().traceId());
    assertThat(child.getParentId()).isEqualTo(parent.context().spanId());
  }

  @Test
  @DisplayName("The error tag carries the exception class only on real failures, never on a business message or an error answer")
  void errorTagOnlyOnRealFailures() throws AWException {
    assertThatThrownBy(() -> observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> {
      throw new AWException("title", "message", AnswerType.WARNING);
    })).isInstanceOf(AWException.class);
    observer.observe(ExecutionType.ACTION, "A1", Map.of(), () -> "list", result -> AnswerType.ERROR);

    assertThat(meterRegistry.get("awe.action").tags("id", "A1", "outcome", "success", "error", "none").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.get("awe.action").tags("id", "A1", "outcome", "error", "error", "none").timer().count()).isEqualTo(1);
  }
}
