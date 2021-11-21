package hs.ddif.core.inject.instantiator;

import java.util.Arrays;

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

  protected static String toCriteriaString(Object... criteria) {
    return criteria.length > 0 ? " with criteria " + Arrays.toString(criteria) : "";
  }

  /**
   * Converts this exception to a {@link RuntimeException}.
   *
   * @return a new {@link RuntimeException}, never null
   */
  public abstract RuntimeException toRuntimeException();
}
