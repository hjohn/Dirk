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

  private List<Class<?>> classes;
  private URLClassLoader classLoader;

  public Plugin(Injector injector, List<Class<?>> classes, URLClassLoader classLoader) {
    this.injector = injector;
    this.classLoader = classLoader;
    this.classes = classes;

    if(classLoader instanceof UnloadTrackingClassLoader) {
      this.unloaded = ((UnloadTrackingClassLoader)classLoader).getUnloadedAtomicBoolean();
    }
    else {
      this.unloaded = null;
    }
  }

  public void unload() {
    Collections.reverse(classes);

    for(Class<?> cls : classes) {
      injector.remove(cls);
    }

    try {
      classLoader.close();
    }
    catch(IOException e) {
      throw new RuntimeException(e);
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
