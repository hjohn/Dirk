package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithUnsupportedOptionalProviderDependency {

  @Nullable @Inject  // @Nullable/@Opt here means provider is allowed to return null (instead of throwing exception)
  private Provider<UnavailableBean> unavailableBeanProvider;

  public Provider<UnavailableBean> getUnavailableBeanProvider() {
    return unavailableBeanProvider;
  }
}
