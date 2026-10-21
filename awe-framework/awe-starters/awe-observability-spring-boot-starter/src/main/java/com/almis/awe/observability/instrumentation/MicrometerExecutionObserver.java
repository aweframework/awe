package com.almis.awe.observability.instrumentation;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.observation.ExecutionObserver;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Observes every AWE query, maintain, action, screen build and export with Micrometer.
 *
 * <p>Each execution is an {@link Observation} named {@code awe.query}, {@code awe.maintain}, {@code awe.action},
 * {@code awe.screen} or {@code awe.export}. The types switched off run plainly, with no observation at all.</p>
 *
 * <p>Spring Boot turns an observation into a timer (count, total time and max) and into a span of the current trace, so
 * metrics and traces share names, tags and outcome, and the span is a child of the current one, which keeps the trace
 * and span ids that the logs already show.</p>
 *
 * <p>Outcome: {@code error} when the execution throws anything but a warning or info {@link AWException}, or ends
 * with an answer of error type (a query or maintain whose data is of error type, an action whose service answered an
 * error and so ran its error answer); otherwise {@code success}, including the warnings and infos of the application.
 * The {@code error} tag is the exception class and appears only when something was really thrown as a failure; it is
 * {@code none} for a success and for an error answer that threw nothing.</p>
 *
 * <p>Tags are low cardinality by construction: {@code id} is the identifier defined in the XML of the application,
 * the extra tags AWE passes ({@code connector} for queries, {@code screen} for screens, {@code format} for exports)
 * come from the XML or have a fixed set of values and {@code outcome} is {@code success} or {@code error}; the
 * {@link AweTagValueLimitFilter} caps the values of {@code id} and {@code screen} anyway. Users and parameters are
 * never tags. The id is a tag of the span too, not part of
 * its name, because Micrometer rewrites span names to lower-case with hyphens and would mangle camel-case ids.</p>
 */
public class MicrometerExecutionObserver implements ExecutionObserver {

  static final String UNDEFINED_ID = "undefined";
  static final String OUTCOME_TAG = "outcome";
  static final String OUTCOME_ERROR = "error";
  static final String OUTCOME_SUCCESS = "success";

  private final Supplier<ObservationRegistry> registry;
  private final Set<ExecutionType> disabledTypes;

  /**
   * Constructor that observes every execution type
   *
   * @param registry Supplier of the observation registry (resolved on use, so the observer can be created before it)
   */
  public MicrometerExecutionObserver(Supplier<ObservationRegistry> registry) {
    this(registry, Set.of());
  }

  /**
   * Constructor
   *
   * @param registry      Supplier of the observation registry (resolved on use, so the observer can be created before it)
   * @param disabledTypes Execution types that run plainly, without observation
   */
  public MicrometerExecutionObserver(Supplier<ObservationRegistry> registry, Set<ExecutionType> disabledTypes) {
    this.registry = registry;
    this.disabledTypes = disabledTypes.isEmpty() ? Set.of() : EnumSet.copyOf(disabledTypes);
  }

  @Override
  public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
    return observe(type, id, tags, execution, null);
  }

  @Override
  public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution,
                       Function<? super T, AnswerType> answerType) throws AWException {
    if (disabledTypes.contains(type)) {
      return execution.execute();
    }
    ObservationRegistry observationRegistry = registry.get();
    if (observationRegistry == null || observationRegistry.isNoop()) {
      return execution.execute();
    }

    Observation observation = Observation.createNotStarted("awe." + type.name().toLowerCase(Locale.ROOT), observationRegistry)
      .lowCardinalityKeyValue("id", id == null ? UNDEFINED_ID : id);
    tags.forEach(observation::lowCardinalityKeyValue);
    observation.start();

    try (Observation.Scope ignored = observation.openScope()) {
      T result = execution.execute();
      observation.lowCardinalityKeyValue(OUTCOME_TAG, isErrorAnswer(result, answerType) ? OUTCOME_ERROR : OUTCOME_SUCCESS);
      return result;
    } catch (AWException exc) {
      // A warning or an info is a message of the application, not a failure: nothing to flag
      if (exc.getType() == null || exc.getType() == AnswerType.ERROR) {
        observation.error(exc);
        observation.lowCardinalityKeyValue(OUTCOME_TAG, OUTCOME_ERROR);
      } else {
        observation.lowCardinalityKeyValue(OUTCOME_TAG, OUTCOME_SUCCESS);
      }
      throw exc;
    } catch (Throwable throwable) {
      observation.error(throwable);
      observation.lowCardinalityKeyValue(OUTCOME_TAG, OUTCOME_ERROR);
      throw throwable;
    } finally {
      observation.stop();
    }
  }

  /**
   * The answer type the execution reports wins; without one, the type of the data of a result decides
   */
  private <T> boolean isErrorAnswer(T result, Function<? super T, AnswerType> answerType) {
    AnswerType type = answerType == null ? null : answerType.apply(result);
    if (type == null && result instanceof ServiceData serviceData) {
      type = serviceData.getType();
    }
    return type == AnswerType.ERROR;
  }
}
