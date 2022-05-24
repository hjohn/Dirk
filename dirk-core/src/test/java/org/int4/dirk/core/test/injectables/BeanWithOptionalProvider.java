package org.int4.dirk.core.test.injectables;

import org.int4.dirk.annotations.Opt;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithOptionalProvider {

  @Inject @Opt
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
