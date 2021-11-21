package hs.ddif.plugins;

import hs.ddif.core.api.CandidateRegistry;

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

import javax.inject.Singleton;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Manages {@link Plugin}s, registering them with a {@link CandidateRegistry} when
 * loaded and removing them when unloaded.
 */
public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final CandidateRegistry baseRegistry;  // the registry to add the plugin classes to, but also may contain required dependencies
  private final PluginScopeResolver pluginScopeResolver;

  /**
   * Constructs a new instance. A {@link CandidateRegistry} must be provided where
   * types part of a {@link Plugin} can be registered and unregistered. A {@link PluginScopeResolver}
   * must be provided to support {@link hs.ddif.annotations.PluginScoped}, a scope which is specific
   * to a plugin (unlike singleton which if defined in a plugin could interfere with other plugins).
   *
   * @param registry a {@link CandidateRegistry}, cannot be null
   * @param pluginScopeResolver a {@link PluginScopeResolver}, cannot be null
   */
  public PluginManager(CandidateRegistry registry, PluginScopeResolver pluginScopeResolver) {
    if(registry == null) {
      throw new IllegalArgumentException("registry cannot be null");
    }
    if(pluginScopeResolver == null) {
      throw new IllegalArgumentException("pluginScopeResolver cannot be null");
    }

    this.baseRegistry = registry;
    this.pluginScopeResolver = pluginScopeResolver;
  }

  /**
   * Scans the given package prefixes and creates a {@link Plugin} for any annotated types located
   * during the scan.
   *
   * @param packageNamePrefixes a list of packages to scan
   * @return a {@link Plugin}, never null
   */
  public Plugin loadPluginAndScan(String... packageNamePrefixes) {
    ClassLoader classLoader = this.getClass().getClassLoader();

    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    return new PluginLoader(ComponentScanner.createReflections(packageNamePrefixes), classLoader)
      .loadPlugin(Arrays.toString(packageNamePrefixes));
  }

  /**
   * Loads jars at the given {@link URL}s, scans for annotated types and creates a {@link Plugin}.
   *
   * @param urls a list of {@link URL}s to load and scan
   * @return a {@link Plugin}, never null
   */
  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    LOGGER.fine("Scanning Plugin at: " + Arrays.toString(urls));

    return new PluginLoader(ComponentScanner.createReflections(urls), classLoader).loadPlugin(Arrays.toString(urls));
  }

  /**
   * Attempts to unload the given plugin. This may fail if not all types can be removed for the
   * underlying {@link CandidateRegistry}.
   *
   * @param plugin a {@link Plugin} to unload, cannot be null
   */
  public void unload(Plugin plugin) {
    baseRegistry.remove(plugin.getTypes());  // may fail
    pluginScopeResolver.unregister(plugin);  // can't fail, so should be done after baseRegistry#remove
    plugin.destroy();  // can't fail
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

    baseRegistry.register(plugin.getTypes());
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

  private class PluginLoader {
    private final Reflections reflections;
    private final ClassLoader classLoader;

    PluginLoader(Reflections reflections, ClassLoader classLoader) {
      this.reflections = reflections;
      this.classLoader = classLoader;
    }

    Plugin loadPlugin(String pluginName) {
      Collection<String> classes = reflections.get(Scanners.TypesAnnotated.with(Singleton.class));

      if(!classes.isEmpty()) {
        throw new IllegalStateException("Plugins should not use @javax.inject.Singleton annotation as this makes it impossible to unload them.  Use @WeakSingleton instead; detected in: " + classes);
      }

      List<Type> types = ComponentScanner.findComponentTypes(reflections, classLoader);

      LOGGER.fine("Registering types: " + types);

      return createPlugin(pluginName, types, classLoader);
    }
  }
}
