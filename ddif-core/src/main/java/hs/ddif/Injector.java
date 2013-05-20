package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.inject.Singleton;

// TODO Named without value is treated differently ... some use field name, others default to empty?
// TODO What about generics support?  @Inject Shop<Book> --> how would that work?
// TODO See if there really is a point in keeping the Interfaces/Superclass chain seperate from Annotation based qualifiers
public class Injector {

  /**
   * InjectableStore used by this Injector.  The Injector will safeguard that this store
   * only contains injectables that can be fully resolved.
   */
  private final InjectableStore store = new InjectableStore(new InjectorStoreConsistencyPolicy());

  /**
   * Map containing the number of times a specific Key (a reference to a specific class
   * with qualifiers) is referenced.
   */
  private final Map<Key, Integer> referenceCounters = new HashMap<>();

  /**
   * Map containing known singleton instances.  Both key and value are weak to prevent the
   * injector from holding on to unused singletons, or to classes that could be unloaded.<p>
   *
   * The key is always a concrete class.
   */
  private final Map<Class<?>, WeakReference<Object>> singletons = new WeakHashMap<>();

  /**
   * Returns an instance of the given class in which all dependencies are
   * injected.
   *
   * @param cls the class
   * @return an instance of the given class
   * @throws NoSuchBeanException when the given class is not registered with this Injector
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getInstance(Class<T> cls) {
    return getInstance(new Key(cls));
  }

  @SuppressWarnings("unchecked")
  protected <T> T getInstance(Key key) {
    Set<Class<?>> concreteClasses = store.resolve(key);

    if(concreteClasses.isEmpty()) {
      throw new NoSuchBeanException(key);
    }
    if(concreteClasses.size() > 1) {
      throw new AmbigiousBeanException(key, concreteClasses);
    }

    try {
      Class<?> concreteClass = concreteClasses.iterator().next();
      boolean isSingleton = concreteClass.getAnnotation(Singleton.class) != null;

      if(isSingleton) {
        WeakReference<Object> reference = singletons.get(concreteClass);

        if(reference != null) {
          T bean = (T)reference.get();  // create strong reference first

          if(bean != null) {  // if it was not null, return it
            return bean;
          }
        }
      }

      Map<AccessibleObject, Binding> injections = store.getInjections(concreteClass);

      /*
       * Look for a constructor injection, and if found use that.  If not, use the default
       * constructor.
       */

      T bean = null;

      for(AccessibleObject accessibleObject : injections.keySet()) {
        if(accessibleObject instanceof Constructor) {
          Constructor<?> constructor = (Constructor<?>)accessibleObject;

          bean = (T)constructor.newInstance((Object[])injections.get(accessibleObject).getValue(this));
        }
      }

      if(bean == null) {
        bean = (T)concreteClass.newInstance();
      }

      /*
       * Do field/method injections.
       */

      for(Map.Entry<AccessibleObject, Binding> entry : store.getInjections(bean.getClass()).entrySet()) {
        try {
          AccessibleObject accessibleObject = entry.getKey();

          if(accessibleObject instanceof Field) {
            Field field = (Field)accessibleObject;

            field.setAccessible(true);
            field.set(bean, entry.getValue().getValue(this));
          }
        }
        catch(IllegalArgumentException e) {
          throw new InjectionException("Incompatible types", e);
        }
        catch(IllegalAccessException e) {
          throw new InjectionException("Illegal access", e);
        }
      }

      /*
       * Store the result if singleton.
       */

      if(isSingleton) {
        singletons.put(concreteClass, new WeakReference<Object>(bean));
      }

      return bean;
    }
    catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  protected Set<Class<?>> getConcreteClasses(Key key) {
    return store.resolve(key);
  }

  private static <T> Set<Set<T>> powerSet(Set<T> originalSet) {
    Set<Set<T>> sets = new HashSet<>();

    if(originalSet.isEmpty()) {
      sets.add(new HashSet<T>());
      return sets;
    }

    List<T> list = new ArrayList<>(originalSet);
    T head = list.get(0);
    Set<T> rest = new HashSet<>(list.subList(1, list.size()));

    for(Set<T> set : powerSet(rest)) {
      Set<T> newSet = new HashSet<>();
      newSet.add(head);
      newSet.addAll(set);
      sets.add(newSet);
      sets.add(set);
    }

    return sets;
  }

  /**
   * Registers a class with this Injector if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given class.<p>
   *
   * If there are unresolvable dependencies, or registering this class
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.
   *
   * @param concreteClass the class to register with the Injector
   * @throws ViolatesSingularDependencyException when the registration of the given class would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvedDependencyException when one or more dependencies of the given class cannot be resolved
   */
  public void register(Class<?> concreteClass) {
    Map<AccessibleObject, Binding> bindings = store.put(concreteClass);

    for(Binding binding : bindings.values()) {
      Key[] keys = binding.getRequiredKeys();

      for(Key key : keys) {
        Integer referenceCounter = referenceCounters.get(key);

        if(referenceCounter == null) {
          referenceCounter = 1;
        }
        else {
          referenceCounter++;
        }

        referenceCounters.put(key, referenceCounter);
      }
    }
  }

  /**
   * Removes a class from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param concreteClass the class to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal of the given class would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Class<?> concreteClass) {
    Map<AccessibleObject, Binding> bindings = store.remove(concreteClass);

    for(Binding binding : bindings.values()) {
      Key[] keys = binding.getRequiredKeys();

      for(Key key : keys) {
        Integer referenceCounter = referenceCounters.remove(key);

        if(referenceCounter == null) {
          throw new RuntimeException("Assertion error");
        }

        referenceCounter--;

        if(referenceCounter > 0) {
          referenceCounters.put(key, referenceCounter);
        }
      }
    }
  }

  private void ensureSingularDependenciesHold(Class<?> concreteClass, Set<Annotation> qualifiers) {
    Set<Set<Annotation>> qualifierSubSets = powerSet(qualifiers);

    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      for(Set<Annotation> qualifierSubSet : qualifierSubSets) {
        Key key = new Key(qualifierSubSet, cls);

        if(referenceCounters.containsKey(key)) {
          throw new ViolatesSingularDependencyException(concreteClass, key, true);
        }
      }
    }
  }

  private static Set<Class<?>> getSuperClassesAndInterfaces(Class<?> cls) {
    List<Class<?>> toScan = new ArrayList<>();
    Set<Class<?>> superClassesAndInterfaces = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Class<?> scanClass = toScan.remove(toScan.size() - 1);
      superClassesAndInterfaces.add(scanClass);

      for(Class<?> iface : scanClass.getInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getSuperclass());
      }
    }

    return superClassesAndInterfaces;
  }

  /**
   * Policy that makes sure the Injector's InjectableStore at all times contains
   * injectables that can be fully resolved.
   */
  protected class InjectorStoreConsistencyPolicy implements StoreConsistencyPolicy {
    @Override
    public void checkAddition(Class<?> concreteClass, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
      ensureSingularDependenciesHold(concreteClass, qualifiers);

      /*
       * Check the created bindings for unresolved or ambigious dependencies:
       */

      for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
        Key[] requiredKeys = entry.getValue().getRequiredKeys();

        for(Key requiredKey : requiredKeys) {
          Set<Class<?>> concreteClasses = store.resolve(requiredKey);

          if(concreteClasses.isEmpty()) {
            throw new UnresolvedDependencyException("Unable to resolve dependency for " + concreteClass + "." + entry.getKey());
          }
          if(concreteClasses.size() > 1) {
            throw new AmbigiousDependencyException(concreteClass, requiredKey, concreteClasses);
          }
        }
      }
    }

    @Override
    public void checkRemoval(Class<?> concreteClass, Set<Annotation> qualifiers) {
      ensureSingularDependenciesHold(concreteClass, qualifiers);
    }
  }
}
