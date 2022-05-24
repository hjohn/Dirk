package org.int4.dirk.core.test.injectables;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithCollectionProvider {

  @Inject
  private Provider<Set<SimpleCollectionItemInterface>> beans;

  public Set<SimpleCollectionItemInterface> getInjectedValues() {
    return beans.get();
  }
}
