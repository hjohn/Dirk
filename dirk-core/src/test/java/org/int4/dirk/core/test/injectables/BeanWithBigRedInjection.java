package org.int4.dirk.core.test.injectables;

import org.int4.dirk.core.test.qualifiers.Big;
import org.int4.dirk.core.test.qualifiers.Red;

import jakarta.inject.Inject;

public class BeanWithBigRedInjection {

  @Inject @Big @Red
  private Object injection;

  public Object getInjectedValue() {
    return injection;
  }
}
