package org.int4.dirk.plugins.test.project;

import javax.inject.Inject;

public class TestAutoDiscoverableInjectAnnotatedDependency {
  private final TestDatabase database;

  @Inject
  public TestAutoDiscoverableInjectAnnotatedDependency(TestDatabase database) {
    this.database = database;
  }

  public TestDatabase getDatabase() {
    return database;
  }
}
