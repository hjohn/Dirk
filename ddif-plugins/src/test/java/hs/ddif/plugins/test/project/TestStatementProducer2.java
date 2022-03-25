package hs.ddif.plugins.test.project;

import hs.ddif.annotations.Produces;

@NotThisOne  // test ignore annotation: ignores this one during scanning, or otherwise there'd be two of these producers
public class TestStatementProducer2 {

  @Produces
  public TestStatement createStatement(TestConnection connection) {
    return connection.createStatement();
  }
}
