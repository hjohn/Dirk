package org.int4.dirk.plugins;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.int4.dirk.api.CandidateRegistry;
import org.int4.dirk.api.definition.AutoDiscoveryException;
import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.api.definition.DependencyException;

/**
 * Manages {@link Plugin}s, registering them with a {@link CandidateRegistry} when
 * loaded and removing them when unloaded.
 */
public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final ComponentScannerFactory componentScannerFactory;
  private final CandidateRegistry baseRegistry;  // the registry to add the plugin classes to, but also may contain required dependencies

  /**
   * Constructs a new instance. A {@link CandidateRegistry} must be provided where
   * types part of a {@link Plugin} can be registered and unregistered.
   *
   * @param componentScannerFactory a {@link ComponentScannerFactory}, cannot be {@code null}
   * @param registry a {@link CandidateRegistry}, cannot be {@code null}
   */
  public PluginManager(ComponentScannerFactory componentScannerFactory, CandidateRegistry registry) {
    this.componentScannerFactory = Objects.requireNonNull(componentScannerFactory, "componentScannerFactory cannot be null");
    this.baseRegistry = Objects.requireNonNull(registry, "registry cannot be null");
  }

  /**
   * Scans the given package prefixes and creates a {@link Plugin} for any annotated types located
   * during the scan.
   *
   * @param packageNamePrefixes a list of packages to scan
   * @return a {@link Plugin}, never {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   * @throws DependencyException when dependencies between registered types cannot be resolved
   */
  public Plugin loadPluginAndScan(String... packageNamePrefixes) throws AutoDiscoveryException, DefinitionException, DependencyException {
    ClassLoader classLoader = this.getClass().getClassLoader();

    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    return new PluginLoader(componentScannerFactory.create(packageNamePrefixes), classLoader)
      .loadPlugin(Arrays.toString(packageNamePrefixes));
  }

  /**
   * Loads jars at the given {@link URL}s, scans for annotated types and creates a {@link Plugin}.
   *
   * @param urls a list of {@link URL}s to load and scan
   * @return a {@link Plugin}, never {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   * @throws DependencyException when dependencies between registered types cannot be resolved
   */
  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL... urls) throws AutoDiscoveryException, DefinitionException, DependencyException {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    LOGGER.fine("Scanning Plugin at: " + Arrays.toString(urls));

    return new PluginLoader(componentScannerFactory.create(urls), classLoader).loadPlugin(Arrays.toString(urls));
  }

  /**
   * Attempts to unload the given plugin. This may fail if not all types can be removed for the
   * underlying {@link CandidateRegistry}.
   *
   * @param plugin a {@link Plugin} to unload, cannot be {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   * @throws DependencyException when dependencies between registered types cannot be resolved
   */
  public void unload(Plugin plugin) throws AutoDiscoveryException, DefinitionException, DependencyException {
    baseRegistry.remove(plugin.getTypes());  // may fail
    plugin.destroy();  // can't fail
  }

  /**
   * Loads classes from a plugin defined by a Module.
   *
   * @param urls one or more jar files
   * @return a {@link Plugin}, never {@code null}
   * @throws AutoDiscoveryException when auto discovery fails to find all required types
   * @throws DefinitionException when a definition problem was encountered
   * @throws DependencyException when dependencies between registered types cannot be resolved
   */
  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL... urls) throws AutoDiscoveryException, DefinitionException, DependencyException {
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

  private Plugin createPlugin(String name, List<Type> types, ClassLoader classLoader) throws AutoDiscoveryException, DefinitionException, DependencyException {
    Plugin plugin = new Plugin(name, types, classLoader);

    baseRegistry.register(plugin.getTypes());

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
    private final ComponentScanner componentScanner;
    private final ClassLoader classLoader;

    PluginLoader(ComponentScanner componentScanner, ClassLoader classLoader) {
      this.componentScanner = componentScanner;
      this.classLoader = classLoader;
    }

    Plugin loadPlugin(String pluginName) throws AutoDiscoveryException, DefinitionException, DependencyException {
      List<Type> types = componentScanner.findComponentTypes(classLoader);

      LOGGER.fine("Registering types: " + types);

      return createPlugin(pluginName, types, classLoader);
    }
  }
}
