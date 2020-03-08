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

import java.io.IOException;
import java.lang.reflect.Constructor;
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

import org.apache.commons.lang3.reflect.TypeUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.scanners.TypeElementsScanner;

public class PluginManager {
  private static final Logger LOGGER = Logger.getLogger(PluginManager.class.getName());

  private final BeanDefinitionStore baseStore;  // the store to add the plugin classes to, but also may contain required dependencies

  public PluginManager(BeanDefinitionStore store) {
    this.baseStore = store;
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
      classNames.addAll(reflections.getStore().get("TypeAnnotationsScanner").get("hs.ddif.core.Producer"));

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

      List<Type> matchingClasses = DependencySorter.getInTopologicalOrder(injectableStore, classInjectables);

      LOGGER.fine("Registering classes with Injector (in order): " + matchingClasses);

      List<Type> registeredClasses = registerTypes(matchingClasses);

      return new Plugin(baseStore, pluginName, registeredClasses, classLoader);
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

          for(Binding binding : classInjectable.getBindings()) {
            if(!binding.isProvider()) {
              Key key = binding.getRequiredKey();

              if(key != null) {
                Type type = key.getType();
                Class<?> typeClass = TypeUtils.getRawType(type, null);

                if(!Modifier.isAbstract(typeClass.getModifiers()) && !baseStore.contains(key.getType(), (Object[])key.getQualifiersAsArray())) {
                  putInStore(store, typeClass);
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
   * @param urls one or more jar files
   * @return a {@link Plugin}, never null
   */
  @SuppressWarnings("resource")
  public Plugin loadPlugin(URL... urls) {
    URLClassLoader classLoader = new UnloadTrackingClassLoader(urls);

    try {
      @SuppressWarnings("unchecked")
      Class<Module> moduleClass = (Class<Module>)classLoader.loadClass("PluginModule");
      Constructor<Module> constructor = moduleClass.getConstructor();
      Module module = constructor.newInstance();
      List<Type> registeredTypes = registerTypes(module.getTypes());

      return new Plugin(baseStore, Arrays.toString(urls), registeredTypes, classLoader);
    }
    catch(ReflectiveOperationException e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw new IllegalStateException(e);
    }
    catch(Exception e) {
      try {
        classLoader.close();
      }
      catch(IOException e2) {
        e.addSuppressed(e2);
      }

      throw e;
    }
  }

  // Registers types with the injector and returns the types actually registered (could be a subset of classes if classes were already registered).
  private List<Type> registerTypes(List<Type> types) {
    List<Type> registeredTypes = new ArrayList<>();

    try {
      for(Type type : types) {
        if(!baseStore.contains(type)) {
          baseStore.register(type);
          registeredTypes.add(type);
        }
      }

      return registeredTypes;
    }
    catch(BindingException | InjectorStoreConsistencyException e) {

      /*
       * Registration failed, rolling back:
       */

      Collections.reverse(registeredTypes);

      for(Type type : registeredTypes) {
        baseStore.remove(type);
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
