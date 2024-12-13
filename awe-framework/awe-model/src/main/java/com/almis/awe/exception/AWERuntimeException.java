package com.almis.awe.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * AWE Runtime exception.
 */
@Slf4j
public class AWERuntimeException extends RuntimeException {

  /** Constructs a new runtime exception with the specified cause and a
   * detail message of {@code (cause==null ? null : cause.toString())}
   * (which typically contains the class and detail message of
   * {@code cause}).  This constructor is useful for runtime exceptions
   * that are little more than wrappers for other throwables.
   *
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A {@code null} value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   * @since  1.4
   */
  public AWERuntimeException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and
   * cause.  <p>Note that the detail message associated with
   * {@code cause} is <i>not</i> automatically incorporated in
   * this runtime exception's detail message.
   *
   * @param  message the detail message (which is saved for later retrieval
   *         by the {@link #getMessage()} method).
   * @param  cause the cause (which is saved for later retrieval by the
   *         {@link #getCause()} method).  (A {@code null} value is
   *         permitted, and indicates that the cause is nonexistent or
   *         unknown.)
   * @since  1.4
   */
  public AWERuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

  /** Constructs a new runtime exception with the specified detail message.
   * The cause is not initialized, and may subsequently be initialized by a
   * call to {@link #initCause}.
   *
   * @param   message   the detail message. The detail message is saved for
   *          later retrieval by the {@link #getMessage()} method.
   */
  public AWERuntimeException(String message) {
    super(message);
  }
}
