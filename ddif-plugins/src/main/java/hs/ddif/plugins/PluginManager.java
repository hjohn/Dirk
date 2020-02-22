package hs.ddif.plugins;

import hs.ddif.core.ProvidedInjectable;
import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.inject.consistency.InjectorStoreConsistencyException;
import hs.ddif.core.inject.store.BeanDefinitionStore;
import hs.ddif.core.inject.store.BindingException;
import hs.ddif.core.inject.store.ClassInjectable;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.TypeUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Provider;

import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;

public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final BeanDefinitionStore store;

  public PluginManager(BeanDefinitionStore store) {
    this.store = store;
  }

  public Plugin loadPluginAndScan(String packageNamePrefix) {
    return loadPluginAndScan(new String[] {packageNamePrefix});
  }

  public Plugin loadPluginAndScan(String... packageNamePrefixes) {
    ClassLoader classLoader = this.getClass().getClassLoader();

    LOGGER.fine("Scanning packages: " + Arrays.toString(packageNamePrefixes));

    Reflections reflections = new Reflections(
      packageNamePrefixes,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner()
    );

    return new PluginLoader(reflections, classLoader).loadPlugin(Arrays.toString(packageNamePrefixes));
  }

  public Plugin loadPluginAndScan(URL url) {
    return loadPluginAndScan(new URL[] {url});
  }

  @SuppressWarnings("resource")
  public Plugin loadPluginAndScan(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    LOGGER.fine("Scanning Plugin at: " + Arrays.toString(urls));

    Reflections reflections = new Reflections(
      urls,
      new TypeAnnotationsScanner(),
      new FieldAnnotationsScanner(),
      new MethodAnnotationsScanner(),
      new TypeElementsScanner()
    );

    return new PluginLoader(reflections, classLoader).loadPlugin(Arrays.toString(urls));
  }

  private class PluginLoader {
    private final Set<ClassInjectable> classInjectables = new HashSet<>();
    private final Reflections reflections;
    private final ClassLoader classLoader;

    public PluginLoader(Reflections reflections, ClassLoader classLoader) {
      this.reflections = reflections;
      this.classLoader = classLoader;
    }

    public Plugin loadPlugin(String pluginName) {
      Set<String> classNames = new HashSet<>();

      classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Named"));
      classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("javax.inject.Singleton"));

      for(String name : reflections.getStore().get("FieldAnnotationsScanner").get("javax.inject.Inject")) {
        classNames.add(name.substring(0, name.lastIndexOf('.')));
      }
      for(String name : reflections.getStore().get("MethodAnnotationsScanner").get("javax.inject.Inject")) {
        name = name.substring(0, name.lastIndexOf('('));
        classNames.add(name.substring(0, name.lastIndexOf('.')));
      }

      InjectableStore<Injectable> injectableStore = new InjectableStore<>();

      LOGGER.fine("Found classes: " + classNames);

      for(String className : classNames) {
        try {
          Class<?> cls = classLoader.loadClass(className);

          if(!Modifier.isAbstract(cls.getModifiers())) {
            putInStore(injectableStore, cls);
          }
        }
        catch(ClassNotFoundException e) {
          throw new IllegalStateException(e);
        }
      }

      List<Class<?>> matchingClasses = DependencySorter.getInTopologicalOrder(injectableStore, classInjectables);

      LOGGER.fine("Registering classes with Injector (in order): " + matchingClasses);

      List<Class<?>> registeredClasses = registerClasses(matchingClasses);

      return new Plugin(store, pluginName, registeredClasses, classLoader);
    }

    private void putInStore(InjectableStore<Injectable> store, Class<?> cls) {
      if(!store.contains(cls)) {
        try {
          ClassInjectable classInjectable = new ClassInjectable(cls);

          store.put(classInjectable);
          classInjectables.add(classInjectable);

          /*
           * Self discovery of other injectables
           */

          for(Binding[] bindings : classInjectable.getBindings().values()) {
            for(Binding binding : bindings) {
              if(!binding.isProvider()) {
                Key key = binding.getRequiredKey();

                if(key != null) {
                  Type type = key.getType();
                  Class<?> typeClass = TypeUtils.determineClassFromType(type);

                  if(!Modifier.isAbstract(typeClass.getModifiers()) && !store.contains(key.getType(), (Object[])key.getQualifiersAsArray())) {
                    putInStore(store, typeClass);
                  }
                }
              }
            }
          }

          /*
           * Self discovery of providers:
           */

          if(Provider.class.isAssignableFrom(cls)) {
            store.put(new ProvidedInjectable(cls));
          }
        }
        catch(Exception e) {
          throw new IllegalStateException("Exception while loading plugin class: " + cls, e);
        }
      }
    }
  }

  public Plugin loadPlugin(URL url) {
    return loadPlugin(new URL[] {url});
  }

  /**
   * Loads classes from a plugin defined by a Module.
   *
   * @param url a jar file
   * @return a {@link Plugin}
   */
  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    try {
      @SuppressWarnings("unchecked")
      Class<Module> moduleClass = (Class<Module>)classLoader.loadClass("PluginModule");
      Module module = moduleClass.newInstance();
      List<Class<?>> registeredClasses = registerClasses(module.getClasses());

      return new Plugin(store, Arrays.toString(urls), registeredClasses, classLoader);
    }
    catch(ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw new IllegalStateException(e);
    }
  }

  // Registers classes with the injector and returns the classes actually registered (could be a subset of classes if classes were already registered).
  private List<Class<?>> registerClasses(List<Class<?>> classes) {
    List<Class<?>> registeredClasses = new ArrayList<>();

    try {
      for(Class<?> cls : classes) {
        if(!store.contains(cls)) {
          store.register(cls);
          registeredClasses.add(cls);
        }
      }

      return registeredClasses;
    }
    catch(BindingException | InjectorStoreConsistencyException e) {

      /*
       * Registration failed, rolling back:
       */

      Collections.reverse(registeredClasses);

      for(Class<?> cls : registeredClasses) {
        store.remove(cls);
      }

      throw e;
    }
  }

  static class UnloadTrackingClassLoader extends URLClassLoader {
    private final AtomicBoolean unloaded = new AtomicBoolean();

    public UnloadTrackingClassLoader(URL[] urls) {
      super(urls);
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
