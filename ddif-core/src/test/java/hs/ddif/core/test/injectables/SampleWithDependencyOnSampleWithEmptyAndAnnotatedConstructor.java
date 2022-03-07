package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor {
  @Inject
  public SampleWithEmptyAndAnnotatedConstructor sample;
}