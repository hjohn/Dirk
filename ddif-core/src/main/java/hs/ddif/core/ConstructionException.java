package hs.ddif.core;

import javax.annotation.PostConstruct;

/**
 * Thrown when during (post-)construction of a dependency a problem
 * occurs.  Constructors that throw an exceptions, {@link PostConstruct}s
 * which trigger further dependency construction which eventually need
 * the current object under construction (causing a loop) and so on.
 */
public class ConstructionException extends DependencyException {

  public ConstructionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ConstructionException(String message) {
    super(message);
  }
}
