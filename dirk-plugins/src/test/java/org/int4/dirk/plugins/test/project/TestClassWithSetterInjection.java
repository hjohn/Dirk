package org.int4.dirk.plugins.test.project;

import javax.inject.Inject;

public class TestClassWithSetterInjection {
  private TestDatabase database;

  @Inject
  public void setDatabase(TestDatabase database) {
    this.database = database;
  }

  public TestDatabase getDatabase() {
    return database;
  }
}
