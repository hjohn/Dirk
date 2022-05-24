package org.int4.dirk.core.test.injectables;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithProvider {

  @Inject
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
