package hs.ddif.core.test.injectables;

import hs.ddif.annotations.Opt;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithOptionalProvider {

  @Inject @Opt
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
