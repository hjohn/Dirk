package hs.ddif.test.injectables;

import javax.inject.Inject;
import javax.inject.Named;

public class BeanWithDirectRedCollectionItemDependency {

  @Inject
  @Named("RED")
  private SimpleCollectionItemInterface collectionItem;

  public SimpleCollectionItemInterface getCollectionItem() {
    return collectionItem;
  }
}
