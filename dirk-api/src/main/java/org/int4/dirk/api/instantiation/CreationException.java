package org.int4.dirk.api.instantiation;

import java.util.Objects;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exception, setters or post constructors
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class CreationException extends InjectionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   * @param cause a {@link Throwable} to use as cause
   */
  public CreationException(String message, Throwable cause) {
    super(Objects.requireNonNull(message, "message"), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public CreationException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
