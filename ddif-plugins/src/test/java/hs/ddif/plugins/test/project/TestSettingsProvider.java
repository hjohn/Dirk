package hs.ddif.plugins.test.project;

import hs.ddif.plugins.test.project.TestParentalControlsProvider.TestParentalControls;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;

public class TestSettingsProvider {
  @SuppressWarnings("unused")
  @Inject private TestSettingsService service;
  @SuppressWarnings("unused")
  @Inject private Provider<TestParentalControls> parentalControlsProvider;
  @Inject @Nullable private Integer unprovided;
}
