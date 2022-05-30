package org.int4.dirk.api.definition;

import java.util.Objects;

/**
 * Thrown when a dependency is not present in the store.
 */
public class MissingDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public MissingDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }

}
