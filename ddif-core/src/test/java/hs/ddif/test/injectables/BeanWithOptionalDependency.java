package hs.ddif.test.injectables;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class BeanWithOptionalDependency {

  @Nullable @Inject
  private UnavailableBean unavailableBean;

  public UnavailableBean getUnavailableBean() {
    return unavailableBean;
  }
}
