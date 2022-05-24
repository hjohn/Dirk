package org.int4.dirk.api.definition;

import java.util.Objects;

/**
 * Thrown when auto discovery is allowed and the types discovered have definition
 * problems or fail to register as a coherent whole.
 *
 * <p>Never thrown when auto discovery is off.
 */
public class AutoDiscoveryException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message describing the problem, cannot be {@code null}
   * @param cause an underlying cause of the problem, cannot be {@code null}
   */
  public AutoDiscoveryException(String message, Throwable cause) {
    super(Objects.requireNonNull(message, "message"), Objects.requireNonNull(cause, "cause"));
  }
}
