package hs.ddif.plugins.test.project;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TestSettingsService {
  @SuppressWarnings("unused")
  @Inject private TestDatabase database;
}
