package hs.ddif.core.test.injectables;

import hs.ddif.core.test.qualifiers.Big;

import jakarta.inject.Inject;

public class BeanWithBigInjection {

  @Inject @Big
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
