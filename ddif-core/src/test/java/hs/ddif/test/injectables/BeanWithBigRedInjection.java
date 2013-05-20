package hs.ddif.test.injectables;

import hs.ddif.test.qualifiers.Big;
import hs.ddif.test.qualifiers.Red;

import javax.inject.Inject;

public class BeanWithBigRedInjection {

  @Inject @Big @Red
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
