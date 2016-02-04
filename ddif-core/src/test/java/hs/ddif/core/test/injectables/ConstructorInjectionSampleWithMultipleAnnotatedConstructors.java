package hs.ddif.core.test.injectables;

import javax.inject.Inject;

/**
 * Sample with multiple constructors annotated with @Inject.
 */
public class ConstructorInjectionSampleWithMultipleAnnotatedConstructors {
  private final SimpleBean simpleBean;

  @Inject
  public ConstructorInjectionSampleWithMultipleAnnotatedConstructors(SimpleBean simpleBean) {
    this.simpleBean = simpleBean;
  }

  @Inject
  public ConstructorInjectionSampleWithMultipleAnnotatedConstructors() {
    this(null);
  }

  public SimpleBean getInjectedValue() {
    return simpleBean;
  }
}
