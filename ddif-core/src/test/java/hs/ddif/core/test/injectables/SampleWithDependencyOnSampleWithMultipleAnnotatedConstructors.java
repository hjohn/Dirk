package hs.ddif.core.test.injectables;

import jakarta.inject.Inject;

public class SampleWithDependencyOnSampleWithMultipleAnnotatedConstructors {
  @Inject
  public SampleWithMultipleAnnotatedConstructors sample;
}