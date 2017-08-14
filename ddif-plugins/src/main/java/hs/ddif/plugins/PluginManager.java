package hs.ddif.plugins;

import hs.ddif.core.Binder;
import hs.ddif.core.Binding;
import hs.ddif.core.ClassInjectable;
import hs.ddif.core.DependencyException;
import hs.ddif.core.InjectableStore;
import hs.ddif.core.Injector;
import hs.ddif.core.Key;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final Injector injector;

  public PluginManager(Injector injector) {
    this.injector = injector;
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(String packageNamePrefix) {
    URLClassLoader classLoader = (URLClassLoader)this.getClass().getClassLoader();

    LOGGER.fine("scanning " + packageNamePrefix);

    Reflections reflections = new Reflections(
      packageNamePrefix,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner()
    );

    return new PluginLoader(reflections, classLoader).loadPlugin();
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL url) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(url);

    LOGGER.fine("scanning " + url);

    Reflections reflections = new Reflections(
      url,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner(),
      new SubTypesScanner(false)
    );

    return new PluginLoader(reflections, classLoader).loadPlugin();
  }

  private class PluginLoader {
    private final Set<ClassInjectable> classInjectables = new HashSet<>();
    private final Reflections reflections;
    private final URLClassLoader classLoader;

    public PluginLoader(Reflections reflections, URLClassLoader classLoader) {
      this.reflections = reflections;
      this.classLoader = classLoader;
    }

    public Plugin loadPlugin() {
      Set<String> classNames = new HashSet<>();

      classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Named"));

      for(String name : reflections.getStore().get("FieldAnnotationsScanner").get("javax.inject.Inject")) {
        classNames.add(name.substring(0, name.lastIndexOf('.')));
      }
      for(String name : reflections.getStore().get("MethodAnnotationsScanner").get("javax.inject.Inject")) {
        name = name.substring(0, name.lastIndexOf('('));
        classNames.add(name.substring(0, name.lastIndexOf('.')));
      }

      InjectableStore store = new InjectableStore();

      for(String className : classNames) {
        LOGGER.finer("found " + className);
        try {
          putInStore(store, classLoader.loadClass(className));
        }
        catch(ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      List<Class<?>> matchingClasses = DependencySorter.getInTopologicalOrder(store, classInjectables);
      List<Class<?>> registeredClasses = registerClasses(matchingClasses);

      return new Plugin(injector, registeredClasses, classLoader);
    }

    private void putInStore(InjectableStore store, Class<?> cls) {
      if(!store.contains(cls) && reflections.getAllTypes().contains(cls.getName())) {
        try {
          LOGGER.finest("adding " + cls.getName());

          ClassInjectable classInjectable = new ClassInjectable(cls);

          store.put(classInjectable);
          classInjectables.add(classInjectable);

          /*
           * Self discovery of other injectables
           */

          for(Binding[] bindings : classInjectable.getBindings().values()) {
            for(Binding binding : bindings) {
              Key key = binding.getRequiredKey();

              if(key != null) {
                Type type = key.getType();
                Class<?> typeClass = Binder.determineClassFromType(type);

                if(!typeClass.isInterface() && !Modifier.isAbstract(typeClass.getModifiers())) {
                  putInStore(store, typeClass);
                }
              }
            }
          }
        }
        catch(Exception e) {
          throw new IllegalStateException("Exception while loading plugin class: " + cls, e);
        }
      }
    }
  }

  /**
   * Loads classes from a plugin defined by a Module.
   *
   * @param url a jar file
   * @return a {@link Plugin}
   */
  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL url) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(url);

    try {
      @SuppressWarnings("unchecked")
      Class<Module> moduleClass = (Class<Module>)classLoader.loadClass("PluginModule");
      Module module = moduleClass.newInstance();
      List<Class<?>> registeredClasses = registerClasses(module.getClasses());

      return new Plugin(injector, registeredClasses, classLoader);
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

  // Registers classes with the injector and returns the classes actually registered (could be a subset of classes if classes were already registered).
  private List<Class<?>> registerClasses(List<Class<?>> classes) {
    List<Class<?>> registeredClasses = new ArrayList<>();

    try {
      for(Class<?> cls : classes) {
        if(!injector.contains(cls)) {
          injector.register(cls);
          registeredClasses.add(cls);
        }
      }

      return registeredClasses;
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
  }

  static class UnloadTrackingClassLoader extends URLClassLoader {
    private final AtomicBoolean unloaded = new AtomicBoolean();

    public UnloadTrackingClassLoader(URL url) {
      super(new URL[] {url});
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
