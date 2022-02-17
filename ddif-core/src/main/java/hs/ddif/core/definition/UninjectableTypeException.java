package hs.ddif.core.definition;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Thrown when a {@link Type} is encountered that is unsuitable for injection.
 */
public class UninjectableTypeException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be {@code null}
   * @param message a message, cannot be {@code null}
   */
  public UninjectableTypeException(Type type, String message) {
    super("[" + Objects.requireNonNull(type) + "] " + Objects.requireNonNull(message));
  }
}
