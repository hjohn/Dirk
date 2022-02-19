package hs.ddif.core.store;

/**
 * Thrown when attempting to add a {@link Key} which already exists in the store.
 */
public class DuplicateKeyException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   */
  public DuplicateKeyException(Key key) {
    super("[" + key + "] is already present");
  }

}
