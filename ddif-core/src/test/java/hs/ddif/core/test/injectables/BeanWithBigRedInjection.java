package hs.ddif.core.test.injectables;

import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;

import jakarta.inject.Inject;

public class BeanWithBigRedInjection {

  @Inject @Big @Red
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
