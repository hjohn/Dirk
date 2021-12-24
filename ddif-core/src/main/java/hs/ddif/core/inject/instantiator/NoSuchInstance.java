package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.store.Criteria;
import hs.ddif.core.store.Key;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstance extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param criteria a {@link Criteria}, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public NoSuchInstance(Key key, Criteria criteria, Throwable cause) {
    super("No such instance: " + key + toCriteriaString(criteria), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param criteria a {@link Criteria}, can be null
   */
  public NoSuchInstance(Key key, Criteria criteria) {
    this(key, criteria, null);
  }

  @Override
  public NoSuchInstanceException toRuntimeException() {
    return new NoSuchInstanceException(getMessage(), getCause());
  }
}
