package hs.ddif.test.injectables;

import hs.ddif.test.qualifiers.Big;

import javax.inject.Inject;

public class BeanWithBigInjection {

  @Inject @Big
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
