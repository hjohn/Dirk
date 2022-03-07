package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class BeanWithDirectRedCollectionItemDependency {

  @Inject
  @Named("RED")
  private SimpleCollectionItemInterface collectionItem;

  public SimpleCollectionItemInterface getCollectionItem() {
    return collectionItem;
  }
}
