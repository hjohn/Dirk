package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class BeanWithInjection {

  @Inject
  private SimpleBean simpleBean;

  public SimpleBean getInjectedValue() {
    return simpleBean;
  }
}
