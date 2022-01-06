package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.Matcher;

import java.util.List;

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

  /**
   * Generates a description for a list of {@link Matcher}s.
   *
   * @param matchers a list of {@link Matcher}s, cannot be null
   * @return a descriptive string, never null
   */
  protected static String toCriteriaString(List<Matcher> matchers) {
    return matchers.isEmpty() ? "" : " with " + matchers;
  }

  /**
   * Converts this exception to a {@link RuntimeException}.
   *
   * @return a new {@link RuntimeException}, never null
   */
  public abstract RuntimeException toRuntimeException();
}
