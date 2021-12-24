package hs.ddif.core.inject.instantiator;

import hs.ddif.core.store.Criteria;

/**
 * Base class for exceptions that can be thrown during instance resolution by
 * an {@link Instantiator}.
 */
public abstract class InstanceResolutionFailure extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public InstanceResolutionFailure(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   */
  public InstanceResolutionFailure(String message) {
    super(message);
  }

  protected static String toCriteriaString(Criteria criteria) {
    return criteria == null || criteria.equals(Criteria.EMPTY) ? "" : " with " + criteria;
  }

  /**
   * Converts this exception to a {@link RuntimeException}.
   *
   * @return a new {@link RuntimeException}, never null
   */
  public abstract RuntimeException toRuntimeException();
}
