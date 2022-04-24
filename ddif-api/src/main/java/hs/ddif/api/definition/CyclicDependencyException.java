package hs.ddif.api.definition;

import java.util.Objects;

/**
 * Thrown when not all dependencies of a class can be resolved.  This occurs when
 * a class requires a specific dependency but no such dependency is available or
 * more than one matching dependency is available.
 */
public class CyclicDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public CyclicDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
