package hs.ddif.core.inject.instantiator;

import java.lang.reflect.Type;

/**
 * Thrown when auto discovery fails. As new injectables may need to be added to
 * the underlying store, consistency checks may fail which this class wraps.
 */
public class DiscoveryFailure extends InstanceCreationFailure {

  /**
   * Creates a new instance.
   *
   * @param type type involved, cannot be null
   * @param message a message describing the problem
   * @param cause an optional cause, can be null
   */
  public DiscoveryFailure(Type type, String message, Throwable cause) {
    super(type, message, cause);
  }
}
