package com.almis.awe.observability.health;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs a blocking call (opening a connection, validating it) with an overall time limit, so a hung database or
 * broker cannot block the health endpoint.
 *
 * <p>Only one call per key (a database alias, the queue) runs at a time: while a call that timed out is still
 * running, later calls with the same key do not start another one and fail with {@link StillRunningException}. So
 * a resource that keeps hanging holds one thread, not one more per health request. Threads are daemons, and a call
 * that does not finish is interrupted.</p>
 */
final class TimeBoundedCall {

  /**
   * The previous call with the same key has not finished yet
   */
  static final class StillRunningException extends Exception {
    private static final long serialVersionUID = 1L;

    StillRunningException() {
      super("check still running", null, false, false);
    }
  }

  private static final AtomicInteger COUNTER = new AtomicInteger();
  private static final ExecutorService SHARED_POOL = Executors.newCachedThreadPool(runnable -> {
    Thread thread = new Thread(runnable, "awe-health-check-" + COUNTER.incrementAndGet());
    thread.setDaemon(true);
    return thread;
  });

  private final ConcurrentMap<Object, AtomicBoolean> running = new ConcurrentHashMap<>();
  private final ExecutorService executor;

  /**
   * Constructor that runs the calls in the shared pool of daemon threads
   */
  TimeBoundedCall() {
    this(SHARED_POOL);
  }

  /**
   * Constructor
   *
   * @param executor Executor that runs the calls
   */
  TimeBoundedCall(ExecutorService executor) {
    this.executor = executor;
  }

  /**
   * Run a call within a time limit
   *
   * @param key     What is being checked (one call at a time per key)
   * @param call    Call to run
   * @param timeout Maximum time to wait for it
   * @param <T>     Result type
   * @return Result of the call
   * @throws StillRunningException If the previous call with this key has not finished
   * @throws TimeoutException      If the call did not finish in time
   * @throws InterruptedException  If the calling thread was interrupted
   * @throws Exception             The exception the call threw
   */
  <T> T run(Object key, Callable<T> call, Duration timeout) throws Exception {
    AtomicBoolean flag = running.computeIfAbsent(key, k -> new AtomicBoolean());
    if (!flag.compareAndSet(false, true)) {
      throw new StillRunningException();
    }
    // Exactly one of the call and the cancellation claims the task, and the claimer releases the key: a call that
    // started releases it when it ends (even if it ignored the interruption), a call cancelled before it started
    // releases it at once because it will never run
    AtomicBoolean claimed = new AtomicBoolean();
    FutureTask<T> task = new FutureTask<>(() -> {
      if (!claimed.compareAndSet(false, true)) {
        return null;
      }
      try {
        return call.call();
      } finally {
        flag.set(false);
      }
    }) {
      @Override
      protected void done() {
        if (isCancelled() && claimed.compareAndSet(false, true)) {
          flag.set(false);
        }
      }
    };
    try {
      executor.execute(task);
    } catch (RejectedExecutionException exc) {
      flag.set(false);
      throw exc;
    }
    try {
      return task.get(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
    } catch (TimeoutException | InterruptedException exc) {
      task.cancel(true);
      throw exc;
    } catch (ExecutionException exc) {
      throw exc.getCause() instanceof Exception cause ? cause : exc;
    }
  }
}
