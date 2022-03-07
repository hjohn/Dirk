package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithProviderWithoutMatch {

  @Inject
  private Provider<SimpleBean> simpleBeanProvider;

  public SimpleBean getSimpleBean() {
    return simpleBeanProvider.get();
  }
}
