package org.int4.dirk.plugins.test.project;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class TestDatabaseSetup implements Provider<TestConnection> {
  private static int index;

  @Override
  public TestConnection get() {
    return new TestConnection("Connection " + index++);
  }
}
