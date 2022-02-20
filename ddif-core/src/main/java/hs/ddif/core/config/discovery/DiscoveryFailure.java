package hs.ddif.core.config.discovery;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.store.Key;

/**
 * Thrown when auto discovery fails. As new injectables may need to be added to
 * the underlying store, consistency checks may fail which this class wraps.
 */
public class DiscoveryFailure extends InstanceCreationFailure {

  /**
   * Creates a new instance.
   *
   * @param key a {@link Key} involved, cannot be {@code null}
   * @param message a message describing the problem
   * @param cause an optional cause, can be {@code null}
   */
  public DiscoveryFailure(Key key, String message, Throwable cause) {
    super(key, message, cause);
  }

  /**
   * Creates a new instance.
   *
   * @param message a message describing the problem
   * @param cause an optional cause, can be {@code null}
   */
  public DiscoveryFailure(String message, Throwable cause) {
    super(message, cause);
  }
}
