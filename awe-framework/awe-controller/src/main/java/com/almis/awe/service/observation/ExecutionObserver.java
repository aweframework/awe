package com.almis.awe.service.observation;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.type.AnswerType;

import java.util.Map;
import java.util.function.Function;

/**
 * Extension point to observe the executions of AWE queries, maintains, actions, screens and exports (metrics, traces,
 * auditing...).
 *
 * <p>AWE calls {@link #observe} around each execution at the point where every way of reaching it converges, so
 * one execution is reported once whatever started it (client request, service, scheduler or listener). When the
 * application defines a bean of this type, AWE uses it; otherwise {@link #NONE} runs the execution untouched.</p>
 *
 * <p>The identifiers are the ones defined in the XML files of the application, never values from a user or a
 * request, so they are safe to use as low-cardinality labels.</p>
 */
public interface ExecutionObserver {

  /**
   * Observer that does nothing: the execution just runs
   */
  ExecutionObserver NONE = new ExecutionObserver() {
    @Override
    public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
      return execution.execute();
    }
  };

  /**
   * Kind of execution
   */
  enum ExecutionType {
    /**
     * Query launched by a query launcher
     */
    QUERY,
    /**
     * Maintain target launched by the maintain service
     */
    MAINTAIN,
    /**
     * Action launched by the action service
     */
    ACTION,
    /**
     * Screen built by the screen service for an option of the menu (the screen opened by the user)
     */
    SCREEN,
    /**
     * Screen report generated in one output format (print or export)
     */
    EXPORT
  }

  /**
   * An execution to be observed
   *
   * @param <T> Result type
   */
  @FunctionalInterface
  interface Execution<T> {
    /**
     * Run the execution
     *
     * @return Result of the execution
     * @throws AWException Error in the execution
     */
    T execute() throws AWException;
  }

  /**
   * Run an execution and observe it. Implementations must run the execution exactly once, return its result and
   * let its exception through.
   *
   * @param type      Kind of execution
   * @param id        Identifier of the query, maintain target, action, option or screen, as defined in the application
   * @param tags      Extra low-cardinality information (for example the query connector)
   * @param execution Execution to run
   * @param <T>       Result type
   * @return Result of the execution
   * @throws AWException Error in the execution
   */
  <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException;

  /**
   * Run an execution and observe it, with the answer type the execution ended with. AWE calls this method. An
   * observer that wants to tell a failure from a business message overrides it; by default it ignores the answer
   * type and calls {@link #observe(ExecutionType, String, Map, Execution)}.
   *
   * <p>The answer type is {@link AnswerType#ERROR} when the execution answered with an error without throwing (a
   * service that answered an error, so an action ran its error answer) and null when the execution has no answer
   * type. An {@link AWException} carries its own type: only {@code ERROR} is a failure, while {@code WARNING} and
   * {@code INFO} are business messages.</p>
   *
   * @param type       Kind of execution
   * @param id         Identifier of the query, maintain target, action, option or screen, as defined in the application
   * @param tags       Extra low-cardinality information (for example the query connector)
   * @param execution  Execution to run
   * @param answerType Answer type of a result of the execution (it may return null)
   * @param <T>        Result type
   * @return Result of the execution
   * @throws AWException Error in the execution
   */
  default <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution,
                        Function<? super T, AnswerType> answerType) throws AWException {
    return observe(type, id, tags, execution);
  }
}
