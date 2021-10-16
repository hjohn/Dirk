package hs.ddif.plugins;

import hs.ddif.core.Injector;
import hs.ddif.core.util.AnnotationDescriptor;

import org.junit.jupiter.api.Test;

public class PluginManagerRegistrationTest {
  private Injector injector = new Injector(true);

  @Test
  public void shouldLoadWithoutException() {
    injector.registerInstance("{jsonconfig}", AnnotationDescriptor.named("configuration"));

    ComponentScanner.scan(
      injector.getStore(),
      "hs.ddif.plugins.test.project"
    );
  }
}
