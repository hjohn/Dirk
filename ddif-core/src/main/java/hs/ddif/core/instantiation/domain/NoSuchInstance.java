package hs.ddif.core.instantiation.domain;

import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.store.Key;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstance extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   */
  public NoSuchInstance(Key key) {
    super("No such instance: [" + key + "]");
  }

  @Override
  public NoSuchInstanceException toRuntimeException() {
    return new NoSuchInstanceException(getMessage(), this);
  }
}
