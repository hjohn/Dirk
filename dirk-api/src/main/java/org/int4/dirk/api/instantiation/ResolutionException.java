package org.int4.dirk.api.instantiation;

/**
 * Base class for exceptions that can be thrown during type safe resolution.
 */
public abstract class ResolutionException extends InjectionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public ResolutionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   */
  public ResolutionException(String message) {
    super(message);
  }
}
