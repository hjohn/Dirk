package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class BeanWithUnresolvedDependency {

  @Inject
  private Runtime runtime;

  public Runtime getRuntime() {
    return runtime;
  }
}
