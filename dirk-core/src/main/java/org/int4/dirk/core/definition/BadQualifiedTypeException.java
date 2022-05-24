package org.int4.dirk.core.definition;

import java.util.Objects;

/**
 * Thrown when a {@link QualifiedType} is encountered that is unsuitable for injection.
 */
public class BadQualifiedTypeException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public BadQualifiedTypeException(String message) {
    super(Objects.requireNonNull(message));
  }
}
