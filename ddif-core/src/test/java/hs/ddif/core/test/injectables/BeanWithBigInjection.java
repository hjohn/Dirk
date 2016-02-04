package hs.ddif.core.test.injectables;

import javax.inject.Inject;

import hs.ddif.core.test.qualifiers.Big;

public class BeanWithBigInjection {

  @Inject @Big
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
