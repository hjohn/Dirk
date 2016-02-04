package hs.ddif.core.test.injectables;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithProvider {

  @Inject
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
