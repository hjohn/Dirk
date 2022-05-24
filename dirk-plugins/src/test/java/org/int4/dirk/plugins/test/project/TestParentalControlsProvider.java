package org.int4.dirk.plugins.test.project;

import java.util.List;

import javax.inject.Singleton;

import org.int4.dirk.plugins.test.project.TestParentalControlsProvider.TestParentalControls;

@Singleton
public class TestParentalControlsProvider extends TestConfigurationProvider<TestParentalControls> {

  public TestParentalControlsProvider() {
    super(TestParentalControls.class, "parental-controls");
  }

  public static class TestParentalControls {
    public final String passcode;
    public final int timeout;
    public final List<String> hidden;

    public TestParentalControls(String passcode, Integer timeout, List<String> hidden) {
      this.passcode = passcode;
      this.timeout = timeout == null ? 900 : timeout;
      this.hidden = hidden;
    }
  }
}