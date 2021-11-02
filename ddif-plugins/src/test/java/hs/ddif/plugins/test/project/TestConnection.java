package hs.ddif.plugins.test.project;

public class TestConnection {
  private final String name;

  public TestConnection(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public TestStatement createStatement() {
    return new TestStatement(this);
  }
}
