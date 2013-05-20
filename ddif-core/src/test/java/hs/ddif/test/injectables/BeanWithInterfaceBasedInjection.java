package hs.ddif.test.injectables;

import javax.inject.Inject;

public class BeanWithInterfaceBasedInjection {

  @Inject
  private SimpleInterface simpleBean;

  public SimpleInterface getInjectedValue() {
    return simpleBean;
  }
}
