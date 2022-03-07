package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithUnsupportedOptionalProviderDependency {

  @Nullable @Inject  // @Nullable/@Opt here means provider is allowed to return null (instead of throwing exception)
  private Provider<UnavailableBean> unavailableBeanProvider;

  public Provider<UnavailableBean> getUnavailableBeanProvider() {
    return unavailableBeanProvider;
  }
}
