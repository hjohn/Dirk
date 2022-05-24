package org.int4.dirk.core.test.injectables;

import org.int4.dirk.core.util.Nullable;

import jakarta.inject.Inject;

public class BeanWithOptionalDependency {

  @Nullable @Inject
  private UnavailableBean unavailableBean;

  public UnavailableBean getUnavailableBean() {
    return unavailableBean;
  }
}
