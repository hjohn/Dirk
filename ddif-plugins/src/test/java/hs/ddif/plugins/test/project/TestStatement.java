package hs.ddif.plugins.test.project;

public class TestStatement {
  private final TestConnection testConnection;

  public TestStatement(TestConnection testConnection) {
    this.testConnection = testConnection;
  }

  public TestConnection getTestConnection() {
    return testConnection;
  }
}
