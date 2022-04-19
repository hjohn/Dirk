package hs.ddif.core.store;

import hs.ddif.core.InjectableFactories;
import hs.ddif.core.definition.Injectable;

public class Injectables {
  private static final InjectableFactories injectableFactories = new InjectableFactories();

  public static Injectable<Object> create(String text) {
    return injectableFactories.forInstance().create(text);
  }
}
