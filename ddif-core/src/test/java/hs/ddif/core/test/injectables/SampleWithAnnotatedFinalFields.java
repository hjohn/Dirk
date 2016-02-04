package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class SampleWithAnnotatedFinalFields {
  @SuppressWarnings("unused")
  @Inject
  private final SampleWithoutConstructor a;

  public SampleWithAnnotatedFinalFields() {
    this.a = null;
  }
}