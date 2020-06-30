package hs.ddif.plugins;

import hs.ddif.core.inject.store.BeanDefinitionStore;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
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

  public PluginManager(BeanDefinitionStore store) {
    this.baseStore = store;
  }

  public Plugin loadPluginAndScan(String packageNamePrefix) {
    return loadPluginAndScan(new String[] {packageNamePrefix});
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

  public Plugin loadPluginAndScan(URL url) {
    return loadPluginAndScan(new URL[] {url});
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
      List<Type> types = ComponentScanner.findComponentTypes(reflections, classLoader);

      LOGGER.fine("Registering types: " + types);

      baseStore.register(types);

      return new Plugin(baseStore, pluginName, types, classLoader);
    }
  }

  public Plugin loadPlugin(URL url) {
    return loadPlugin(new URL[] {url});
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

      baseStore.register(module.getTypes());

      return new Plugin(baseStore, Arrays.toString(urls), module.getTypes(), classLoader);
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

  static class UnloadTrackingClassLoader extends URLClassLoader {
    private final AtomicBoolean unloaded = new AtomicBoolean();

    public UnloadTrackingClassLoader(URL[] urls) {
      super(urls);
    }

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
