package hs.ddif.core.test.injectables;

import javax.inject.Inject;
import javax.inject.Provider;

public class BeanWithUnresolvedProviderDependency {

  @Inject
  private Provider<Runtime> runtime;

  public Runtime getRuntime() {
    return runtime.get();
  }
}
