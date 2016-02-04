package hs.ddif.scan;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import hs.ddif.core.Injector;

public class Plugin {
  private final AtomicBoolean unloaded;
  private final Injector injector;

  private List<Class<?>> classes;
  private URLClassLoader classLoader;

  public Plugin(Injector injector, List<Class<?>> classes, URLClassLoader classLoader, AtomicBoolean unloaded) {
    this.injector = injector;
    this.classLoader = classLoader;
    this.unloaded = unloaded;
    this.classes = classes;
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
    return unloaded.get();
  }
}
