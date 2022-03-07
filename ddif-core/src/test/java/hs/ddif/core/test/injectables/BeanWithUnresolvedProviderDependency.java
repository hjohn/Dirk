package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class BeanWithUnresolvedProviderDependency {

  @Inject
  private Provider<Runtime> runtime;

  public Runtime getRuntime() {
    return runtime.get();
  }
}
