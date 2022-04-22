package hs.ddif.api.instantiation;

import java.util.Objects;

/**
 * Thrown when multiple matching instances were available.
 */
public class AmbiguousResolutionException extends ResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public AmbiguousResolutionException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
