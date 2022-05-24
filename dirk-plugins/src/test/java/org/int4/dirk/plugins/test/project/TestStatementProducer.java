package org.int4.dirk.plugins.test.project;

import org.int4.dirk.annotations.Produces;

public class TestStatementProducer {

  @Produces
  public TestStatement createStatement(TestConnection connection) {
    return connection.createStatement();
  }
}
