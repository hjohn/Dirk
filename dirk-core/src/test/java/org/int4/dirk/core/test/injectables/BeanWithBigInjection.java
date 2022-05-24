package org.int4.dirk.core.test.injectables;

import org.int4.dirk.core.test.qualifiers.Big;

import jakarta.inject.Inject;

public class BeanWithBigInjection {

  @Inject @Big
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
