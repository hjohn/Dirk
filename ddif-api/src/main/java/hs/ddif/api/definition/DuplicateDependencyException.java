package hs.ddif.api.definition;

import java.util.Objects;

/**
 * Thrown when attempting to add a dependency which was already added to the store.
 */
public class DuplicateDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public DuplicateDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }

}
