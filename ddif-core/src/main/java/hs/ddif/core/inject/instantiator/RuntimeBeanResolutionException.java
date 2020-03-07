package hs.ddif.core.inject.instantiator;

import hs.ddif.core.store.Injectable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

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

  public RuntimeBeanResolutionException(Type type, Throwable cause, Object... criteria) {
    super("No such bean: " + type + toCriteriaString(criteria), cause);
  }

  public RuntimeBeanResolutionException(Type type, Object... criteria) {
    super("No such bean: " + type + toCriteriaString(criteria));
  }

  public RuntimeBeanResolutionException(Set<? extends Injectable> injectables, Type type, Object... criteria) {
    super("Multiple matching beans: " + type + toCriteriaString(criteria) + ": " + injectables);
  }

  private static String toCriteriaString(Object... criteria) {
    return criteria.length > 0 ? " with criteria " + Arrays.toString(criteria) : "";
  }
}
