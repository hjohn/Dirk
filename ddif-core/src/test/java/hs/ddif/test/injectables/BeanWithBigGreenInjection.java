package hs.ddif.test.injectables;

import hs.ddif.test.qualifiers.Green;

import javax.inject.Inject;

public class BeanWithBigGreenInjection {

  @Inject @Green
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
