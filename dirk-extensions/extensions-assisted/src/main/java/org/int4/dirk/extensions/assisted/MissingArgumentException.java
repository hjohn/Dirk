package org.int4.dirk.extensions.assisted;

import java.util.Objects;

/**
 * Thrown by an {@link AssistedAnnotationStrategy} when determining an argument
 * name for a method, field or parameter fails.
 */
public class MissingArgumentException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public MissingArgumentException(String message) {
    super(Objects.requireNonNull(message, "message cannot be null"));
  }

}
