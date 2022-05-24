package org.int4.dirk.api.definition;

import java.util.Objects;

/**
 * Thrown when not all dependencies can be resolved.  This occurs when
 * a type requires a specific dependency but no such dependency is available.
 */
public class UnsatisfiedDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public UnsatisfiedDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
