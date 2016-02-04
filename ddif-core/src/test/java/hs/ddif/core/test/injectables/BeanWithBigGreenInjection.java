package hs.ddif.core.test.injectables;

import javax.inject.Inject;

import hs.ddif.core.test.qualifiers.Green;

public class BeanWithBigGreenInjection {

  @Inject @Green
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
