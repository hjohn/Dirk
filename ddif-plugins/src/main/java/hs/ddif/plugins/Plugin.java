package hs.ddif.plugins;

import hs.ddif.core.Injector;
import hs.ddif.plugins.PluginManager.UnloadTrackingClassLoader;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Plugin {
  private final AtomicBoolean unloaded;
  private final Injector injector;
  private final String name;

  private List<Class<?>> classes;
  private ClassLoader classLoader;

  public Plugin(Injector injector, String name, List<Class<?>> classes, ClassLoader classLoader) {
    this.injector = injector;
    this.name = name;
    this.classLoader = classLoader;
    this.classes = classes;

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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for(Class<?> cls : classes) {
      sb.append(" - ").append(cls.getName()).append("\n");
    }

    return "Plugin: " + name + "\n" + sb.toString();
  }

  public void unload() {
    Collections.reverse(classes);

    for(Class<?> cls : classes) {
      injector.remove(cls);
    }

    try {
      if(classLoader instanceof URLClassLoader) {
        ((URLClassLoader)classLoader).close();
      }
    }
    catch(IOException e) {
      throw new IllegalStateException(e);
    }
    finally {
      classes.clear();
      classes = null;
      classLoader = null;
    }
  }

  public boolean isUnloaded() {
    return unloaded == null ? false : unloaded.get();
  }
}
