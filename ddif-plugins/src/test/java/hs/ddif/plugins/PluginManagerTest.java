package hs.ddif.plugins;

import hs.ddif.core.AmbigiousDependencyException;
import hs.ddif.core.Injector;
import hs.ddif.core.JustInTimeDiscoveryPolicy;
import hs.ddif.core.NoSuchBeanException;
import hs.ddif.core.ViolatesSingularDependencyException;
import hs.ddif.test.plugin.Database;
import hs.ddif.test.plugin.TextProvider;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

  @Before
  public void before() {
    injector = new Injector(new JustInTimeDiscoveryPolicy());
    pluginManager = new PluginManager(injector);

    injector.register(BeanWithTextProviders.class);
  }

  @AfterClass
  public static void afterClass() {
    try {
      Thread.sleep(250);
    }
    catch(InterruptedException e) {
    }
  }

  @Test
  public void shouldLoadThenUnloadPlugin() {
    BeanWithTextProviders beanBefore = injector.getInstance(BeanWithTextProviders.class);

    Plugin plugin = pluginManager.loadPluginAndScan(PLUGIN_URL);

    BeanWithTextProviders bean = injector.getInstance(BeanWithTextProviders.class);

    List<String> texts = extractTextsFromBeanWithTextProviders(bean);

    assertThat(texts, containsInAnyOrder("Fancy Text", "NORMAL TEXT", ">>Styled Text<<"));

    plugin.unload();

    BeanWithTextProviders beanAfter = injector.getInstance(BeanWithTextProviders.class);

    assertEquals(0, beanBefore.getTextProviders().size());
    assertEquals(3, bean.getTextProviders().size());
    assertEquals(0, beanAfter.getTextProviders().size());

    gc();

    assertFalse(plugin.isUnloaded());

    bean = null;

    waitForPluginUnload(plugin);

    assertTrue(plugin.isUnloaded());
  }

  @Test
  public void shouldLoadPluginAgainAfterUnload() {
    assertBeanDoesNotExist(Database.class);

    Plugin plugin = pluginManager.loadPluginAndScan(PLUGIN_URL);

    Database db1 = injector.getInstance(Database.class);

    plugin.unload();

    assertBeanDoesNotExist(Database.class);

    plugin = pluginManager.loadPlugin(PLUGIN_URL);

    Database db2 = injector.getInstance(Database.class);

    plugin.unload();

    assertBeanDoesNotExist(Database.class);

    assertNotNull(db1);
    assertNotNull(db2);
    assertFalse(db1.getClass().equals(db2.getClass()));
    assertFalse(db1.getClass().getClassLoader().equals(db2.getClass().getClassLoader()));
    assertTrue(db1.getClass().getName().equals(db2.getClass().getName()));
  }

  @Test(expected = ViolatesSingularDependencyException.class)
  public void shouldNotLoadPluginWhenLoadingWouldViolateSingularDependencies() {
    injector.register(DatabaseBean.class);  // Provides Database
    injector.register(BeanWithDatabase.class);  // Requires an unambigious Database dependency

    pluginManager.loadPlugin(PLUGIN_URL);  // Provides Database as well, but fails as BeanWithDatabase's dependency would become ambigious
  }

  @Test
  public void shouldLoadPluginAfterFixingSingularDependencyViolations() {
    injector.register(DatabaseBean.class);
    injector.register(BeanWithDatabase.class);  // Requires an unambigious Database dependency

    try {
      pluginManager.loadPlugin(PLUGIN_URL);
      fail();
    }
    catch(ViolatesSingularDependencyException e) {
    }

    injector.remove(BeanWithDatabase.class);  // Removes the requirement on an unambigious Database

    pluginManager.loadPlugin(PLUGIN_URL);  // Plugin now loads

    try {
      injector.register(BeanWithDatabase.class);  // Fails, requires an unambigious Database dependency
      fail();
    }
    catch(AmbigiousDependencyException e) {
    }

    injector.remove(DatabaseBean.class);  // Removes one of the Database beans
    injector.register(BeanWithDatabase.class);  // Succeeds, requires an unambigious Database dependency

    Assert.assertNotNull(injector.getInstance(BeanWithDatabase.class));
  }


  private void assertBeanDoesNotExist(Class<?> beanClass) {
    try {
      injector.getInstance(beanClass);
      fail();
    }
    catch(NoSuchBeanException e) {
    }
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
