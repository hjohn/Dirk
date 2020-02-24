package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import javax.inject.Inject;

public class BeanWithOptionalDependency {

  @Nullable @Inject
  private UnavailableBean unavailableBean;

  public UnavailableBean getUnavailableBean() {
    return unavailableBean;
  }
}
