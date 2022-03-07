package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class FieldInjectionSampleWithAnnotatedFinalField {

  @Inject
  private final SimpleBean injectedValue;

  public FieldInjectionSampleWithAnnotatedFinalField(SimpleBean injectedValue) {
    this.injectedValue = injectedValue;
  }

  public FieldInjectionSampleWithAnnotatedFinalField() {
    this(null);
  }

  public SimpleBean getInjectedValue() {
    return injectedValue;
  }
}
