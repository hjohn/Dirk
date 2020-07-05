package hs.ddif.plugins;

import hs.ddif.plugins.PluginManager.UnloadTrackingClassLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Plugin {
  private final AtomicBoolean unloaded;
  private final String name;

  private List<Type> types;
  private ClassLoader classLoader;

  public Plugin(String name, List<Type> types, ClassLoader classLoader) {
    this.name = name;
    this.classLoader = classLoader;
    this.types = Collections.unmodifiableList(types);

    if(classLoader instanceof UnloadTrackingClassLoader) {
      this.unloaded = ((UnloadTrackingClassLoader)classLoader).getUnloadedAtomicBoolean();
    }
    else {
      this.unloaded = null;
    }
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

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

  public boolean isUnloaded() {
    return unloaded == null ? false : unloaded.get();
  }
}
