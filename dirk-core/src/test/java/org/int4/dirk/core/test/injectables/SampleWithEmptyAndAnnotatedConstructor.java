package org.int4.dirk.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithEmptyAndAnnotatedConstructor {

  public SampleWithEmptyAndAnnotatedConstructor() {
  }

  @Inject
  public SampleWithEmptyAndAnnotatedConstructor(@SuppressWarnings("unused") SampleWithoutConstructor parameter) {
  }
}