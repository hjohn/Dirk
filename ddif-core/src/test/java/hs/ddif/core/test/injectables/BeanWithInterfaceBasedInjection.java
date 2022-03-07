package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class BeanWithInterfaceBasedInjection {

  @Inject
  private SimpleInterface simpleBean;

  public SimpleInterface getInjectedValue() {
    return simpleBean;
  }
}
