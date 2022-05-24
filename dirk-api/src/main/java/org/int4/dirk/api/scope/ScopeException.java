package org.int4.dirk.api.scope;

import java.util.Objects;

/**
 * Thrown when for scope related problems.
 */
public abstract class ScopeException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public ScopeException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
