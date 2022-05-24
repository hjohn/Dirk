package org.int4.dirk.core.test.injectables;

import org.int4.dirk.core.util.Nullable;

import jakarta.inject.Inject;

public class BeanWithOptionalConstructorDependency {

  @Inject
  public BeanWithOptionalConstructorDependency(@SuppressWarnings("unused") @Nullable UnavailableBean unavailableBean) {
  }
}
