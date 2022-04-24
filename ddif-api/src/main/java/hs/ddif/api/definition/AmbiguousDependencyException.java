package hs.ddif.api.definition;

import java.util.Objects;

/**
 * Thrown when there are multiple candidates for a dependency where only one is expected.
 * This occurs when a type requires a specific dependency and multiple options are available.
 */
public class AmbiguousDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public AmbiguousDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
