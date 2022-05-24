package org.int4.dirk.core.test.injectables;

import java.util.Set;

import jakarta.inject.Inject;

public class BeanWithCollection {

  @Inject
  private Set<SimpleCollectionItemInterface> beans;

  public Set<SimpleCollectionItemInterface> getInjectedValues() {
    return beans;
  }
}
