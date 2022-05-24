package org.int4.dirk.api.instantiation;

/**
 * Base class for exceptions that can be thrown during injection.
 */
public abstract class InjectionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public InjectionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   */
  public InjectionException(String message) {
    super(message);
  }
}
