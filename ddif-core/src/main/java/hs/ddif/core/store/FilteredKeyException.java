package hs.ddif.core.store;

import java.util.Objects;

/**
 * Thrown when attempting to store an object in the {@link QualifiedTypeStore} which directly
 * matched the configured class filter.
 */
public class FilteredKeyException extends RuntimeException {
  private final Key key;
  private final Object injectable;

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key} that was filtered, cannot be {code null}
   * @param injectable an injectable object, cannot be {@code null}
   */
  public FilteredKeyException(Key key, Object injectable) {
    super("[" + key + "] cannot be added to or removed from the store as it matched the class filter");

    this.key = Objects.requireNonNull(key, "key cannot be null");
    this.injectable = Objects.requireNonNull(injectable, "injectable cannot be null");
  }

  /**
   * Returns the {@link Key}.
   *
   * @return a {@link Key}, never {@code null}
   */
  public Key getKey() {
    return key;
  }

  /**
   * Returns the injectable object.
   *
   * @return an injectable object, never {@code null}
   */
  public Object getInjectable() {
    return injectable;
  }
}
