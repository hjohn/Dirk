package org.int4.dirk.core.test.injectables;

import jakarta.inject.Inject;

public class BeanWithDirectCollectionItemDependency {

  @Inject
  private SimpleCollectionItemInterface collectionItem;

  public SimpleCollectionItemInterface getCollectionItem() {
    return collectionItem;
  }
}
