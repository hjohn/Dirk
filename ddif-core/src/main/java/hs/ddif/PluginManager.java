package hs.ddif;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reflections.Reflections;

public class PluginManager {
  private final Injector injector;

  public PluginManager(Injector injector) {
    this.injector = injector;
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL url) {
    AtomicBoolean unloaded = new AtomicBoolean();
    URLClassLoader classLoader = new UnloadTrackingClassLoader(url, unloaded);

    Reflections reflections = new Reflections(url);

    Collection<String> classNames = reflections.getStore().getStoreMap().get("TypeAnnotationsScanner").get("javax.inject.Named");

    InjectableStore store = new InjectableStore();

    for(String className : classNames) {
      try {
        store.put(classLoader.loadClass(className));
      }
      catch(ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    List<Class<?>> matchingClasses = DependencySorter.getInTopologicalOrder(store);
    List<Class<?>> registeredClasses = new ArrayList<>();

    try {
      for(Class<?> cls : matchingClasses) {
        injector.register(cls);
        registeredClasses.add(cls);
      }
    }
    catch(DependencyException e) {

      /*
       * Registration failed, rolling back:
       */

      Collections.reverse(registeredClasses);

      for(Class<?> cls : registeredClasses) {
        injector.remove(cls);
      }

      throw e;
    }

    return new Plugin(injector, registeredClasses, classLoader, unloaded);
  }

  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL url) {
    AtomicBoolean unloaded = new AtomicBoolean();
    URLClassLoader classLoader = new UnloadTrackingClassLoader(url, unloaded);//URLClassLoader.newInstance(new URL[] {url});

    try {
      @SuppressWarnings("unchecked")
      Class<Module> moduleClass = (Class<Module>)classLoader.loadClass("PluginModule");
      Module module = moduleClass.newInstance();
      List<Class<?>> registeredClasses = new ArrayList<>();

      try {
        for(Class<?> cls : module.getClasses()) {
          injector.register(cls);
          registeredClasses.add(cls);
        }
      }
      catch(DependencyException e) {

        /*
         * Registration failed, rolling back:
         */

        Collections.reverse(registeredClasses);

        for(Class<?> cls : registeredClasses) {
          injector.remove(cls);
        }

        throw e;
      }

      return new Plugin(injector, registeredClasses, classLoader, unloaded);
    }
    catch(ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw new RuntimeException(e);
    }
  }

  private static class UnloadTrackingClassLoader extends URLClassLoader {
    private final AtomicBoolean unloaded;

    public UnloadTrackingClassLoader(URL url, AtomicBoolean unloaded) {
      super(new URL[] {url});

      this.unloaded = unloaded;
    }

    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      unloaded.set(true);
    }
  }
}
