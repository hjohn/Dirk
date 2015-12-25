package hs.ddif;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final Injector injector;

  public PluginManager(Injector injector) {
    this.injector = injector;
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL url) {
    AtomicBoolean unloaded = new AtomicBoolean();
    URLClassLoader classLoader = new UnloadTrackingClassLoader(url, unloaded);

    LOGGER.fine("scanning " + url);

    Reflections reflections = new Reflections(
      url,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner()
    );

    Set<String> classNames = new HashSet<>();

    classNames.addAll(reflections.getStore().getStoreMap().get("TypeAnnotationsScanner").get("javax.inject.Named"));

    for(String name : reflections.getStore().getStoreMap().get("FieldAnnotationsScanner").get("javax.inject.Inject")) {
      classNames.add(name.substring(0, name.lastIndexOf('.')));
    }
    for(String name : reflections.getStore().getStoreMap().get("MethodAnnotationsScanner").get("javax.inject.Inject")) {
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

    List<Class<?>> matchingClasses = DependencySorter.getInTopologicalOrder(store);
    List<Class<?>> registeredClasses = new ArrayList<>();

    try {
      for(Class<?> cls : matchingClasses) {
        if(!injector.contains(cls)) {
          injector.register(cls);
          registeredClasses.add(cls);
        }
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

  private void putInStore(InjectableStore store, Class<?> cls) {
    if(!store.contains(cls)) {
      LOGGER.finest("adding " + cls.getName());

      Map<AccessibleObject, Binding> bindings = store.put(new ClassInjectable(cls));

      /*
       * Self discovery of other injectables
       */

      for(Binding binding : bindings.values()) {
        for(Key key : binding.getRequiredKeys()) {
          Type type = key.getType();
          Class<?> typeClass = Binder.determineClassFromType(type);

          if(!typeClass.isInterface() && !Modifier.isAbstract(typeClass.getModifiers())) {
            putInStore(store, typeClass);
          }
        }
      }
    }
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
