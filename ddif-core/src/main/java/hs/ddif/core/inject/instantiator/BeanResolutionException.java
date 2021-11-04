package hs.ddif.core.inject.instantiator;

import hs.ddif.core.store.Injectable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

/**
 * Thrown when obtaining an instance from an {@link Instantiator} that cannot be supplied,
 * either because there was no such instance or there were multiple matches.
 */
public class BeanResolutionException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param cause a {@link Throwable} cause, can be null
   * @param criteria a list of criteria
   */
  public BeanResolutionException(Type type, Throwable cause, Object... criteria) {
    super("No such bean: " + type + toCriteriaString(criteria), cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param criteria a list of criteria
   */
  public BeanResolutionException(Type type, Object... criteria) {
    super("No such bean: " + type + toCriteriaString(criteria));
  }

  /**
   * Constructs a new instance.
   *
   * @param injectables a set of {@link Injectable}s, cannot be null
   * @param type a {@link Type}, cannot be null
   * @param criteria a list of criteria
   */
  public BeanResolutionException(Set<? extends Injectable> injectables, Type type, Object... criteria) {
    super("Multiple matching beans: " + type + toCriteriaString(criteria) + ": " + injectables);
  }

  private static String toCriteriaString(Object... criteria) {
    return criteria.length > 0 ? " with criteria " + Arrays.toString(criteria) : "";
  }
}
