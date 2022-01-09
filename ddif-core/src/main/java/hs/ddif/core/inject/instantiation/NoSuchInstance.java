package hs.ddif.core.inject.instantiation;

import hs.ddif.core.api.NoSuchInstanceException;
import hs.ddif.core.store.Key;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstance extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param matchers a list of {@link Predicate}s, cannot be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public NoSuchInstance(Key key, List<Predicate<Type>> matchers, Throwable cause) {
    super("No such instance: " + key + toCriteriaString(matchers), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param key a {@link Key}, cannot be {@code null}
   * @param matchers a list of {@link Predicate}s, cannot be {@code null}
   */
  public NoSuchInstance(Key key, List<Predicate<Type>> matchers) {
    this(key, matchers, null);
  }

  @Override
  public NoSuchInstanceException toRuntimeException() {
    return new NoSuchInstanceException(getMessage(), getCause());
  }
}
