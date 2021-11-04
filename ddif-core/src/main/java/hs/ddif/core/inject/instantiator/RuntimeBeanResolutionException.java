package hs.ddif.core.inject.instantiator;

import java.lang.reflect.Type;
import java.util.Arrays;

import javax.inject.Provider;

/**
 * Thrown when obtaining an instance through a {@link Provider} from an {@link Instantiator}
 * that cannot be supplied, either because there was no such instance or there were multiple
 * matches.<p>
 *
 * This exception is equivalent to {@link BeanResolutionException} but not checked as the
 * {@link Provider} interface does not declare it.
 */
public class RuntimeBeanResolutionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Type}, cannot be null
   * @param cause a {@link Throwable} cause, can be null
   * @param criteria a list of criteria
   */
  public RuntimeBeanResolutionException(Type type, Throwable cause, Object... criteria) {
    super("No such bean: " + type + toCriteriaString(criteria), cause);
  }

  private static String toCriteriaString(Object... criteria) {
    return criteria.length > 0 ? " with criteria " + Arrays.toString(criteria) : "";
  }
}
