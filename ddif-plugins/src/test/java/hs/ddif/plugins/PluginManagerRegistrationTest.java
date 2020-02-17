package hs.ddif.plugins;

import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.util.AnnotationDescriptor;

import org.junit.Test;

public class PluginManagerRegistrationTest {
  private Injector injector = new Injector(new JustInTimeDiscoveryPolicy());

  @Test
  public void shouldLoadWithoutException() {
    injector.registerInstance("{jsonconfig}", AnnotationDescriptor.named("configuration"));

    PluginManager pm = new PluginManager(injector);

    pm.loadPluginAndScan("hs.ddif.plugins.test.project");
  }
}
