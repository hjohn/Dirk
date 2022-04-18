package hs.ddif.plugins;

import hs.ddif.api.Injector;
import hs.ddif.api.definition.AutoDiscoveryException;
import hs.ddif.core.inject.store.UnresolvableDependencyException;
import hs.ddif.core.inject.store.ViolatesSingularDependencyException;
import hs.ddif.jsr330.Injectors;
import hs.ddif.test.plugin.Database;
import hs.ddif.test.plugin.TextProvider;
import hs.ddif.test.plugin.TextStyler;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PluginManagerTest {
  private static final URL PLUGIN_URL;

  static {
    try {
      PLUGIN_URL = PluginManagerTest.class.getResource("/plugins/ddif-test-plugin-1.0.0-SNAPSHOT.jar").toURI().toURL();
    }
    catch(MalformedURLException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private Injector injector;
  private PluginManager pluginManager;

  @BeforeEach
  public void beforeEach() throws Exception {
    injector = Injectors.autoDiscovering();
    pluginManager = new PluginManager(new DefaultComponentScannerFactory(), injector.getCandidateRegistry());

    injector.register(BeanWithTextProviders.class);
  }

  @AfterAll
  public static void afterAll() {
    try {
      Thread.sleep(250);
    }
    catch(InterruptedException e) {
    }
  }

  @Test
  public void shouldLoadThenUnloadPlugin() throws Exception {
    BeanWithTextProviders bean1 = injector.getInstance(BeanWithTextProviders.class);  // it's perfectly fine to get a bean with no text providers

    assertNotNull(bean1);
    assertThat(bean1.getTextProviders()).isEmpty();

    injector.register(TextStyler.class);  // not part of plugin, so needs to be registered separate -- don't want it part of plugin either as then plugin would be unable to unload itself

    Plugin plugin = pluginManager.loadPluginAndScan(PLUGIN_URL);

    BeanWithTextProviders bean2 = injector.getInstance(BeanWithTextProviders.class);

    assertNotNull(bean2);
    assertThat(bean2.getTextProviders()).hasSize(3);

    List<String> texts = extractTextsFromBeanWithTextProviders(bean2);

    assertThat(texts).containsExactlyInAnyOrder("Fancy Text", "NORMAL TEXT", ">>Styled Text<<");

    pluginManager.unload(plugin);

    BeanWithTextProviders bean3 = injector.getInstance(BeanWithTextProviders.class);

    assertNotNull(bean3);
    assertThat(bean3.getTextProviders()).isEmpty();

    gc();

    assertFalse(plugin.isUnloaded());  // can't unload as long as we hold a reference to bean2 which has all the text providers

    bean2 = null;

    waitForPluginUnload(plugin);

    assertTrue(plugin.isUnloaded());
    assertTrue(injector.contains(TextStyler.class));  // assert that this didn't get unregistered
  }

  @Test
  public void shouldLoadPluginAgainAfterUnload() throws Exception {
    assertThrows(AutoDiscoveryException.class, () -> injector.getInstance(Database.class));

    injector.register(TextStyler.class);  // not part of plugin, so needs to be registered separate -- don't want it part of plugin either as then plugin would be unable to unload itself

    Plugin plugin = pluginManager.loadPluginAndScan(PLUGIN_URL);

    Database db1 = injector.getInstance(Database.class);

    pluginManager.unload(plugin);

    assertThrows(AutoDiscoveryException.class, () -> injector.getInstance(Database.class));

    plugin = pluginManager.loadPlugin(PLUGIN_URL);

    Database db2 = injector.getInstance(Database.class);

    pluginManager.unload(plugin);

    assertThrows(AutoDiscoveryException.class, () -> injector.getInstance(Database.class));

    assertNotNull(db1);
    assertNotNull(db2);
    assertFalse(db1.getClass().equals(db2.getClass()));
    assertFalse(db1.getClass().getClassLoader().equals(db2.getClass().getClassLoader()));
    assertTrue(db1.getClass().getName().equals(db2.getClass().getName()));
    assertTrue(injector.contains(TextStyler.class));  // assert that this didn't get unregistered
  }

  @Test
  public void shouldNotLoadPluginWhenLoadingWouldViolateSingularDependencies() throws Exception {
    injector.register(DatabaseBean.class);  // Provides Database
    injector.register(BeanWithDatabase.class);  // Requires an unambiguous Database dependency

    assertThrows(ViolatesSingularDependencyException.class, () -> pluginManager.loadPlugin(PLUGIN_URL));  // Provides Database as well, but fails as BeanWithDatabase's dependency would become ambigious
  }

  @Test
  public void shouldLoadPluginAfterFixingSingularDependencyViolations() throws Exception {
    injector.register(DatabaseBean.class);
    injector.register(BeanWithDatabase.class);  // Requires an unambiguous Database dependency

    try {
      pluginManager.loadPlugin(PLUGIN_URL);
      fail();
    }
    catch(ViolatesSingularDependencyException e) {
    }

    injector.remove(BeanWithDatabase.class);  // Removes the requirement on an unambiguous Database

    pluginManager.loadPlugin(PLUGIN_URL);  // Plugin now loads

    try {
      injector.register(BeanWithDatabase.class);  // Fails, requires an unambiguous Database dependency
      fail();
    }
    catch(UnresolvableDependencyException e) {
    }

    injector.remove(DatabaseBean.class);  // Removes one of the Database beans
    injector.register(BeanWithDatabase.class);  // Succeeds, requires an unambiguous Database dependency

    assertNotNull(injector.getInstance(BeanWithDatabase.class));
  }

  private static List<String> extractTextsFromBeanWithTextProviders(BeanWithTextProviders bean) {
    List<String> texts = new ArrayList<>();

    for(TextProvider textProvider : bean.getTextProviders()) {
      texts.add(textProvider.provideText());
    }

    return texts;
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

  private static void gc() {
    for(int i = 0; i < 10; i++) {
      System.gc();

      try {
        Thread.sleep(100);
      }
      catch(InterruptedException e) {
      }
    }
  }
}
