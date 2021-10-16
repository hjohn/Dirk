package hs.ddif.plugins;

import hs.ddif.core.inject.store.BeanDefinitionStore;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;

public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final BeanDefinitionStore baseStore;  // the store to add the plugin classes to, but also may contain required dependencies
  private final PluginScopeResolver pluginScopeResolver;

  public PluginManager(BeanDefinitionStore store, PluginScopeResolver pluginScopeResolver) {
    this.baseStore = store;
    this.pluginScopeResolver = pluginScopeResolver;
  }

  public Plugin loadPluginAndScan(String... packageNamePrefixes) {
    ClassLoader classLoader = this.getClass().getClassLoader();

    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    Reflections reflections = new Reflections(
      packageNamePrefixes,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner()
    );

    return new PluginLoader(reflections, classLoader).loadPlugin(Arrays.toString(packageNamePrefixes));
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    LOGGER.fine("Scanning Plugin at: " + Arrays.toString(urls));

    Reflections reflections = new Reflections(
      urls,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner(),
      new TypeElementsScanner()
    );

    return new PluginLoader(reflections, classLoader).loadPlugin(Arrays.toString(urls));
  }

  private class PluginLoader {
    private final Reflections reflections;
    private final ClassLoader classLoader;

    public PluginLoader(Reflections reflections, ClassLoader classLoader) {
      this.reflections = reflections;
      this.classLoader = classLoader;
    }

    public Plugin loadPlugin(String pluginName) {
      Collection<String> classNames = reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Singleton");

      if(!classNames.isEmpty()) {
        throw new IllegalStateException("Plugins should not use @javax.inject.Singleton annotation as this makes it impossible to unload them.  Use @WeakSingleton instead; detected in: " + classNames);
      }

      List<Type> types = ComponentScanner.findComponentTypes(reflections, classLoader);

      LOGGER.fine("Registering types: " + types);

      return createPlugin(pluginName, types, classLoader);
    }
  }

  public Plugin loadPlugin(URL url) {
    return loadPlugin(new URL[] {url});
  }

  public void unload(Plugin plugin) {
    pluginScopeResolver.unregister(plugin);
    baseStore.remove(plugin.getTypes());
    plugin.destroy();
  }

  /**
   * Loads classes from a plugin defined by a Module.
   *
   * @param urls one or more jar files
   * @return a {@link Plugin}, never null
   */
  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    try {
      @SuppressWarnings("unchecked")
      Class<Module> moduleClass = (Class<Module>)classLoader.loadClass("PluginModule");
      Constructor<Module> constructor = moduleClass.getConstructor();
      Module module = constructor.newInstance();

      return createPlugin(Arrays.toString(urls), module.getTypes(), classLoader);
    }
    catch(ReflectiveOperationException e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw new IllegalStateException(e);
    }
    catch(Exception e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw e;
    }
  }

  private Plugin createPlugin(String name, List<Type> types, ClassLoader classLoader) {
    Plugin plugin = new Plugin(name, types, classLoader);

    baseStore.register(plugin.getTypes());
    pluginScopeResolver.register(plugin);

    return plugin;
  }

  static class UnloadTrackingClassLoader extends URLClassLoader {
    private final AtomicBoolean unloaded = new AtomicBoolean();

    public UnloadTrackingClassLoader(URL[] urls) {
      super(urls);
    }

    public UnloadTrackingClassLoader(URL[] urls, ClassLoader parent) {
      super(urls, parent);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      unloaded.set(true);
    }

    public AtomicBoolean getUnloadedAtomicBoolean() {
      return unloaded;
    }
  }
}
