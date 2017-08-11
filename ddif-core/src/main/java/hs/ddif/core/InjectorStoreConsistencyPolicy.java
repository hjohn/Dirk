package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

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
  public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);

    Map<AccessibleObject, Binding> bindings = injectable.getBindings();

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
  public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
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

  private void ensureSingularDependenciesHold(Type type, Set<AnnotationDescriptor> qualifiers) {
    for(Key key : referenceCounters.keySet()) {
      if(TypeUtils.isAssignable(type, key.getType())) {
        if(qualifiers.containsAll(key.getQualifiers())) {
          throw new ViolatesSingularDependencyException(type, key, true);
        }
      }
    }
  }
}
