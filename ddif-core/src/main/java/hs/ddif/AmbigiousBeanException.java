package hs.ddif;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

public class AmbigiousBeanException extends RuntimeException {

  public AmbigiousBeanException(Set<Injectable> injectables, Type type, Object... criteria) {
    super("Multiple matching beans found [" + injectables.size() + "] for " + type + (criteria.length > 0 ? " matching criteria " + Arrays.toString(criteria) : ""));
  }

}
