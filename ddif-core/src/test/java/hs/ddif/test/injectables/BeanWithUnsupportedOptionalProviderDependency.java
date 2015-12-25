package hs.ddif.test.injectables;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithUnsupportedOptionalProviderDependency {

  @Nullable @Inject  // @Nullable here has no impact on how a Provider works
  private Provider<UnavailableBean> unavailableBeanProvider;

  public Provider<UnavailableBean> getUnavailableBeanProvider() {
    return unavailableBeanProvider;
  }
}
