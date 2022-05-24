package org.int4.dirk.api.definition;

import java.util.Objects;

/**
 * Thrown when an attempt is made to register a type that would cause a
 * dependency required by another type to become ambiguous.
 */
public class AmbiguousRequiredDependencyException extends RequiredDependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public AmbiguousRequiredDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
