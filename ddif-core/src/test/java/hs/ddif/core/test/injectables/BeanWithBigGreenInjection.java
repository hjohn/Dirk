package hs.ddif.core.test.injectables;

import hs.ddif.core.test.qualifiers.Green;

import jakarta.inject.Inject;

public class BeanWithBigGreenInjection {

  @Inject @Green
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
