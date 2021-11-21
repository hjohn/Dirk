package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.NoSuchInstanceException;

import java.lang.reflect.Type;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstance extends InstanceResolutionFailure {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param criteria an array of criteria
   * @param cause a {@link Throwable} cause, can be null
   */
  public NoSuchInstance(Type type, Object[] criteria, Throwable cause) {
    super("No such instance: " + type + toCriteriaString(criteria), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param criteria an array of criteria
   */
  public NoSuchInstance(Type type, Object[] criteria) {
    this(type, criteria, null);
  }

  @Override
  public NoSuchInstanceException toRuntimeException() {
    return new NoSuchInstanceException(getMessage(), getCause());
  }
}
