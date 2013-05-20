package hs.ddif.test.injectables;

import javax.inject.Inject;

public class BeanWithDirectCollectionItemDependency {

  @Inject
  private SimpleCollectionItemInterface collectionItem;

  public SimpleCollectionItemInterface getCollectionItem() {
    return collectionItem;
  }
}
