package hs.ddif.core.test.injectables;

import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

public class BeanWithCollectionProvider {

  @Inject
  private Supplier<Set<SimpleCollectionItemInterface>> beans;

  public Set<SimpleCollectionItemInterface> getInjectedValues() {
    return beans.get();
  }
}
