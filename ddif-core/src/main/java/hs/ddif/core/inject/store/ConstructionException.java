package hs.ddif.core.inject.store;

import javax.annotation.PostConstruct;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exception, {@link PostConstruct}s
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class ConstructionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a {@link Throwable} to use as cause
   */
  public ConstructionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message
   */
  public ConstructionException(String message) {
    super(message);
  }
}
