package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class SampleWithDependencyOnSampleWithEmptyAndAnnotatedConstructor {
  @Inject
  public SampleWithEmptyAndAnnotatedConstructor sample;
}