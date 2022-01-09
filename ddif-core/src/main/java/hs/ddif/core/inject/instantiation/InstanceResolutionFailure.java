package hs.ddif.core.inject.instantiation;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;

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
   * Generates a description for a list of {@link Predicate}s.
   *
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return a descriptive string, never null
   */
  protected static String toCriteriaString(List<Predicate<Type>> matchers) {
    return matchers.isEmpty() ? "" : " with " + matchers;
  }

  /**
   * Converts this exception to a {@link RuntimeException}.
   *
   * @return a new {@link RuntimeException}, never null
   */
  public abstract RuntimeException toRuntimeException();
}
