package hs.ddif.plugins;

import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.plugins.PluginManager.UnloadTrackingClassLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Plugin {
  private final AtomicBoolean unloaded;
  private final BeanDefinitionStore store;
  private final String name;

  private List<Type> types;
  private ClassLoader classLoader;

  public Plugin(BeanDefinitionStore store, String name, List<Type> types, ClassLoader classLoader) {
    this.store = store;
    this.name = name;
    this.classLoader = classLoader;
    this.types = types;

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

    for(Type type : types) {
      sb.append(" - ").append(type.toString()).append("\n");
    }

    return "Plugin: " + name + "\n" + sb.toString();
  }

  public void unload() {
    Collections.reverse(types);

    for(Type type : types) {
      store.remove(type);
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
      types.clear();
      types = null;
      classLoader = null;
    }
  }

  public boolean isUnloaded() {
    return unloaded == null ? false : unloaded.get();
  }
}
