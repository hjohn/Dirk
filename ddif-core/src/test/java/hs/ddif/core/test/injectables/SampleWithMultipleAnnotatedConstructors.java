package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithMultipleAnnotatedConstructors {

  @Inject
  public SampleWithMultipleAnnotatedConstructors() {
  }

  @Inject
  public SampleWithMultipleAnnotatedConstructors(@SuppressWarnings("unused") SampleWithoutConstructor parameter) {
  }
}