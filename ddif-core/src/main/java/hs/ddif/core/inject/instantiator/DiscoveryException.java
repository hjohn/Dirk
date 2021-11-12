package hs.ddif.core.inject.instantiator;

import java.lang.reflect.Type;

/**
 * Thrown when auto discovery fails. As new injectables may need to be added to
 * the underlying store, consistency checks may fail which this exception wraps.
 */
public class DiscoveryException extends InstantiationException {

  /**
   * Creates a new instance.
   *
   * @param type type involved, cannot be null
   * @param message a message describing the problem
   * @param cause an optional cause, can be null
   */
  public DiscoveryException(Type type, String message, Throwable cause) {
    super(type, message, cause);
  }
}
