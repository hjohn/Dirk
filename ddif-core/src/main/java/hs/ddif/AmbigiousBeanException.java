package hs.ddif;

import java.util.Set;

public class AmbigiousBeanException extends RuntimeException {

  public AmbigiousBeanException(Key key, Set<Injectable> injectables) {
    super("Multiple matching beans found [" + injectables.size() + "] for " + key);
  }

}
