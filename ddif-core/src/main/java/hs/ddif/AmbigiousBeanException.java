package hs.ddif;

import java.util.Set;

public class AmbigiousBeanException extends RuntimeException {

  public AmbigiousBeanException(Key key, Set<Class<?>> concreteClasses) {
    super("Multiple matching beans found [" + concreteClasses.size() + "] for " + key);
  }

}
