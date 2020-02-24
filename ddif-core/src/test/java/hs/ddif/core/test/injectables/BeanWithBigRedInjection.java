package hs.ddif.core.test.injectables;

import javax.inject.Inject;

import hs.ddif.core.test.qualifiers.Big;
import hs.ddif.core.test.qualifiers.Red;

public class BeanWithBigRedInjection {

  @Inject @Big @Red
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
