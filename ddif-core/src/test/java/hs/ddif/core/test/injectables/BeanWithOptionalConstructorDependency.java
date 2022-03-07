package hs.ddif.core.test.injectables;

import hs.ddif.core.util.Nullable;

import jakarta.inject.Inject;

public class BeanWithOptionalConstructorDependency {

  @Inject
  public BeanWithOptionalConstructorDependency(@SuppressWarnings("unused") @Nullable UnavailableBean unavailableBean) {
  }
}
