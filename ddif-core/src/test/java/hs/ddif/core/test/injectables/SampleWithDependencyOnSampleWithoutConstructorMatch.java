package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithDependencyOnSampleWithoutConstructorMatch {
  @Inject
  public SampleWithoutConstructorMatch sampleWithoutConstructorMatch;
}