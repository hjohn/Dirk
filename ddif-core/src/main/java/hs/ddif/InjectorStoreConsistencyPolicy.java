package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Policy that makes sure the InjectableStore at all times contains
 * injectables that can be fully resolved.
 */
public class InjectorStoreConsistencyPolicy implements StoreConsistencyPolicy {

  /**
   * Map containing the number of times a specific Key (a reference to a specific class
   * with qualifiers) is referenced.
   */
  private final Map<Key, Integer> referenceCounters = new HashMap<>();

  @Override
  public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);

    if(!injectable.canBeInstantiated(bindings)) {
      throw new DependencyException("No injectable constructor found.  Either annotate one with @Inject or provide a public default constructor: " + injectable.getInjectableClass());
    }

    /*
     * Check the created bindings for unresolved or ambigious dependencies:
     */

    for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
      Key[] requiredKeys = entry.getValue().getRequiredKeys();

      for(Key requiredKey : requiredKeys) {
        Set<Injectable> injectables = injectableStore.resolve(requiredKey);

        if(injectables.isEmpty()) {
          throw new UnresolvedDependencyException(injectable, entry.getKey(), requiredKey);
        }
        if(injectables.size() > 1) {
          throw new AmbigiousDependencyException(injectable.getInjectableClass(), requiredKey, injectables);
        }
      }
    }
  }

  @Override
  public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);
  }

  void addReferences(Key[] keys) {
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

  void removeReferences(Key[] keys) {
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

  private void ensureSingularDependenciesHold(Class<?> classOrInterface, Set<Annotation> qualifiers) {
    Set<Set<Annotation>> qualifierSubSets = powerSet(qualifiers);

    for(Class<?> cls : getSuperClassesAndInterfaces(classOrInterface)) {
      for(Set<Annotation> qualifierSubSet : qualifierSubSets) {
        Key key = new Key(qualifierSubSet, cls);

        if(referenceCounters.containsKey(key)) {
          throw new ViolatesSingularDependencyException(classOrInterface, key, true);
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
}
