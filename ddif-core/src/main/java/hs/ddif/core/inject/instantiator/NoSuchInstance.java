package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.Matcher;
import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.store.Key;

import java.util.List;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstance extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param matchers a list of {@link Matcher}s, cannot be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public NoSuchInstance(Key key, List<Matcher> matchers, Throwable cause) {
    super("No such instance: " + key + toCriteriaString(matchers), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be null
   * @param matchers a list of {@link Matcher}s, cannot be null
   */
  public NoSuchInstance(Key key, List<Matcher> matchers) {
    this(key, matchers, null);
  }

  @Override
  public NoSuchInstanceException toRuntimeException() {
    return new NoSuchInstanceException(getMessage(), getCause());
  }
}
