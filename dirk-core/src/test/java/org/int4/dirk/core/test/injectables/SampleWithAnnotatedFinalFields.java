package org.int4.dirk.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithAnnotatedFinalFields {
  @Inject
  private final SampleWithoutConstructor a;

  public SampleWithAnnotatedFinalFields() {
    this.a = null;
  }
}