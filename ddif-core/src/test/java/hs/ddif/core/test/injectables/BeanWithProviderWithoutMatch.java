package hs.ddif.core.test.injectables;

import java.util.function.Supplier;

import jakarta.inject.Inject;

public class BeanWithProviderWithoutMatch {

  @Inject
  private Supplier<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
