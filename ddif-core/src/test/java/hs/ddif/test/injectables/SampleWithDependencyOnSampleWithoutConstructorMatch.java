package hs.ddif.test.injectables;

import javax.inject.Inject;

public class SampleWithDependencyOnSampleWithoutConstructorMatch {
  @Inject
  public SampleWithoutConstructorMatch sampleWithoutConstructorMatch;
}