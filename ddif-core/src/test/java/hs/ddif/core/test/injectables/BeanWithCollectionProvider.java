package hs.ddif.core.test.injectables;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithCollectionProvider {

  @Inject
  private Provider<Set<SimpleCollectionItemInterface>> beans;

  public Set<SimpleCollectionItemInterface> getInjectedValues() {
    return beans.get();
  }
}
