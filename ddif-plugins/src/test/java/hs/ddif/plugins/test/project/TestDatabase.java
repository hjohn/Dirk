package hs.ddif.plugins.test.project;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class TestDatabase {
  @SuppressWarnings("unused")
  @Inject private Provider<TestConnection> connectionProvider;
}
