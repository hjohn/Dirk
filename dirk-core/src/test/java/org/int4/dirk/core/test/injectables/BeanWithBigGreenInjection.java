package org.int4.dirk.core.test.injectables;

import org.int4.dirk.core.test.qualifiers.Green;

import jakarta.inject.Inject;

public class BeanWithBigGreenInjection {

  @Inject @Green
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
