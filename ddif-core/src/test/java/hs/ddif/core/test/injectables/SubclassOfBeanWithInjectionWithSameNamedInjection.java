package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class SubclassOfBeanWithInjectionWithSameNamedInjection extends BeanWithInjection {

  @Inject
  private SimpleBean simpleBean;

  public SimpleBean getInjectedValueInSubClass() {
    return simpleBean;
  }
}
