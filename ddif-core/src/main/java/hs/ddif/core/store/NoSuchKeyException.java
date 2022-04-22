package hs.ddif.core.store;

import hs.ddif.spi.instantiation.Key;

/**
 * Thrown when a {@link Key} could not be found.
 */
public class NoSuchKeyException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   */
  public NoSuchKeyException(Key key) {
    super("No such key: " + key);
  }

}
