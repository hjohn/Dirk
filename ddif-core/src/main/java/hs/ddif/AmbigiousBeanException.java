package hs.ddif;

import java.util.Arrays;
import java.util.Set;

public class AmbigiousBeanException extends RuntimeException {

  public AmbigiousBeanException(Set<Injectable> injectables, Class<?> concreteClass, Object... criteria) {
    super("Multiple matching beans found [" + injectables.size() + "] for " + concreteClass + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : ""));
  }

}
