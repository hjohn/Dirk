package hs.ddif.plugins;

import hs.ddif.plugins.PluginManager.UnloadTrackingClassLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A list of types with a name and {@link ClassLoader} together representing
 * a plugin that can be loaded or unloaded as a whole.
 */
public class Plugin {
  private final AtomicBoolean unloaded;
  private final String name;

  private List<Type> types;
  private ClassLoader classLoader;

  /**
   * Constructs a new instance.
   *
   * @param name a name for the plugin
   * @param types a list of {@link Type}s part of the plugin, cannot be {@code null} or contains {@code null}s
   * @param classLoader a {@link ClassLoader}, cannot be {@code null}
   */
  public Plugin(String name, List<Type> types, ClassLoader classLoader) {
    if(types == null) {
      throw new IllegalArgumentException("types cannot be null");
    }
    if(classLoader == null) {
      throw new IllegalArgumentException("classLoader cannot be null");
    }

    this.name = name;
    this.classLoader = classLoader;
    this.types = Collections.unmodifiableList(new ArrayList<>(types));

    if(this.types.contains(null)) {
      throw new IllegalArgumentException("types cannot contain null");
    }

    if(classLoader instanceof UnloadTrackingClassLoader) {
      this.unloaded = ((UnloadTrackingClassLoader)classLoader).getUnloadedAtomicBoolean();
    }
    else {
      this.unloaded = null;
    }
  }

  /**
   * Returns the {@link ClassLoader} if the plugin was not unloaded.
   *
   * @return the {@link ClassLoader} or {@code null} if unloaded
   */
  public ClassLoader getClassLoader() {
    return classLoader;
  }

  /**
   * Returns a list of {@link Type}s if the plugin was not unloaded.
   *
   * @return a list of {@link Type}s, immutable and never contains null, or, returns {@code null} if unloaded
   */
  public List<Type> getTypes() {
    return types;
  }

  @Override
  public String toString() {
    return "Plugin[" + name + " -> " + types + "]";
  }

  void destroy() {
    try {
      if(classLoader instanceof URLClassLoader) {
        ((URLClassLoader)classLoader).close();
      }
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      types = null;
      classLoader = null;
    }
  }

  /**
   * Returns {@code true} if this plugin is unloaded, otherwise {@code false}.
   *
   * @return {@code true} if this plugin is unloaded, otherwise {@code false}
   */
  public boolean isUnloaded() {
    return unloaded == null ? false : unloaded.get();
  }
}
