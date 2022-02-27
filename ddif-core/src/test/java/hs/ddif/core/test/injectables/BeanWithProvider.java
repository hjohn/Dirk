package hs.ddif.core.test.injectables;

import java.util.function.Supplier;

import javax.inject.Inject;

public class BeanWithProvider {

  @Inject
  private Supplier<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
