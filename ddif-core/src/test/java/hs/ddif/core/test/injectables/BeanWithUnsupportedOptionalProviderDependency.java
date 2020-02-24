package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithUnsupportedOptionalProviderDependency {

  @Nullable @Inject  // @Nullable here has no impact on how a Provider works
  private Provider<UnavailableBean> unavailableBeanProvider;

  public Provider<UnavailableBean> getUnavailableBeanProvider() {
    return unavailableBeanProvider;
  }
}
