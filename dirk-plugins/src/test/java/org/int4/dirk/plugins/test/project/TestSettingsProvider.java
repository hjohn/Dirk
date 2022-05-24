package org.int4.dirk.plugins.test.project;

import javax.inject.Inject;
import javax.inject.Provider;

import org.int4.dirk.annotations.Opt;
import org.int4.dirk.plugins.test.project.TestParentalControlsProvider.TestParentalControls;

public class TestSettingsProvider {
  @SuppressWarnings("unused")
  @Inject private TestSettingsService service;
  @SuppressWarnings("unused")
  @Inject private Provider<TestParentalControls> parentalControlsProvider;
  @Inject @Opt private Integer unprovided;
  @SuppressWarnings("unused")
  @Inject private TestAutoDiscoverableDependency dependency;
}
