package hs.ddif.core;

import hs.ddif.core.store.Injectable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

/**
 * Thrown when multiple beans are available but only one was expected.
 */
public class AmbigiousBeanException extends RuntimeException {

  public AmbigiousBeanException(Set<? extends Injectable> injectables, Type type, Object... criteria) {
    super("Multiple matching beans found [" + injectables.size() + "] for " + type + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : ""));
  }

}
