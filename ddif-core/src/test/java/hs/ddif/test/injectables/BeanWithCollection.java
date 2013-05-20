package hs.ddif.test.injectables;

import java.util.Set;

import javax.inject.Inject;

public class BeanWithCollection {

  @Inject
  private Set<SimpleCollectionItemInterface> beans;

  public Set<SimpleCollectionItemInterface> getInjectedValues() {
    return beans;
  }
}
