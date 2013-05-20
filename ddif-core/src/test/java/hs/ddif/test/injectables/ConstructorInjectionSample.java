package hs.ddif.test.injectables;

import javax.inject.Inject;

/**
 * Sample with one constructor annotated with @Inject.
 */
public class ConstructorInjectionSample {
  private final SimpleBean simpleBean;

  @Inject
  public ConstructorInjectionSample(SimpleBean simpleBean) {
    this.simpleBean = simpleBean;
  }

  public SimpleBean getInjectedValue() {
    return simpleBean;
  }
}
