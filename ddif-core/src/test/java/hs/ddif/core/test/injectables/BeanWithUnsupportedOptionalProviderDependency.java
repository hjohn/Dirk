package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import java.util.function.Supplier;

import jakarta.inject.Inject;

public class BeanWithUnsupportedOptionalProviderDependency {

  @Nullable @Inject  // @Nullable/@Opt here means provider is allowed to return null (instead of throwing exception)
  private Supplier<UnavailableBean> unavailableBeanProvider;

  public Supplier<UnavailableBean> getUnavailableBeanProvider() {
    return unavailableBeanProvider;
  }
}
