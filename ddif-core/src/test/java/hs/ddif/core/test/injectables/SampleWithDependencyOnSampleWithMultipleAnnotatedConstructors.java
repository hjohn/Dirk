package hs.ddif.core.test.injectables;

import javax.inject.Inject;

public class SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors {
  @Inject
  public SampleWithMultipleAnnotatedConstructors sample;
}