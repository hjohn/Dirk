package org.int4.dirk.api.definition;

import java.util.Objects;

/**
 * Thrown when an attempt is made to remove a type that would cause a
 * dependency required by another type to become unresolvable.
 */
public class UnsatisfiedRequiredDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public UnsatisfiedRequiredDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
