package hs.ddif.core.config.gather;

import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.store.Key;

/**
 * Thrown when auto discovery fails. As new injectables may need to be added to
 * the underlying store, consistency checks may fail which this class wraps.
 */
public class DiscoveryFailure extends InstanceCreationFailure {

  /**
   * Creates a new instance.
   *
   * @param key a {@link Key} involved, cannot be null
   * @param message a message describing the problem
   * @param cause an optional cause, can be null
   */
  public DiscoveryFailure(Key key, String message, Throwable cause) {
    super(key, message, cause);
  }
}
