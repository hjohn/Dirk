package hs.ddif.plugins.test.project;

import hs.ddif.annotations.Produces;

public class TestStatementProducer {

  @Produces
  public TestStatement createStatement(TestConnection connection) {
    return connection.createStatement();
  }
}
