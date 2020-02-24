package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class SampleWithEmptyAndAnnotatedConstructor {

  public SampleWithEmptyAndAnnotatedConstructor() {
  }

  @Inject
  public SampleWithEmptyAndAnnotatedConstructor(@SuppressWarnings("unused") SampleWithoutConstructor parameter) {
  }
}