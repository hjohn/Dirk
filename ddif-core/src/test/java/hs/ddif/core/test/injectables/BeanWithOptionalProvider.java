package hs.ddif.core.test.injectables;

import hs.ddif.annotations.Opt;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithOptionalProvider {

  @Inject @Opt
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
