package hs.ddif.plugins;

import hs.ddif.api.Injector;
import hs.ddif.api.instantiation.domain.InstanceCreationException;
import hs.ddif.jsr330.Injectors;
import hs.ddif.test.plugin.Database;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginSingletonTest {
  private static final URL PLUGIN_URL;

  static {
    try {
      PLUGIN_URL = PluginManagerTest.class.getResource("/plugins/ddif-test-plugin-singleton-1.0.0-SNAPSHOT.jar").toURI().toURL();
    }
    catch(MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void shouldLoadAndUnloadPluginWithPluginSingleton() {
    Injector injector = Injectors.autoDiscovering();

    PluginManager pluginManager = new PluginManager(new DefaultComponentScannerFactory(), injector.getCandidateRegistry());

    for(int i = 0; i < 5; i++) {
      Plugin plugin = pluginManager.loadPluginAndScan(PLUGIN_URL);

      assertNotNull(injector.getInstance(Database.class));

      pluginManager.unload(plugin);

      assertThrows(InstanceCreationException.class, () -> injector.getInstance(Database.class));

      waitForPluginUnload(plugin);

      assertTrue(plugin.isUnloaded());
    }
  }

  private static void waitForPluginUnload(Plugin plugin) {
    for(int i = 0; i < 20; i++) {
      System.gc();

      if(plugin.isUnloaded()) {
        break;
      }

      try {
        Thread.sleep(100);
      }
      catch(InterruptedException e) {
      }
    }
  }
}
