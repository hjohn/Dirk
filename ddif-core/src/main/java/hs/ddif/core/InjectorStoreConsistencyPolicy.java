package hs.ddif.core;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
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
  public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers, Map<AccessibleObject, Binding> bindings) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);

    if(injectable.needsInjection()) {

      /*
       * Check bindings to see if this injectable can be instantiated and injected.
       */

      int constructorCount = 0;

      for(Map.Entry<AccessibleObject, Binding> entry : bindings.entrySet()) {
        if(entry.getKey() instanceof Constructor) {
          constructorCount++;
        }
        if(entry.getKey() instanceof Field) {
          Field field = (Field)entry.getKey();

          if(Modifier.isFinal(field.getModifiers())) {
            throw new BindingException("Cannot inject final field: " + field + " in: " + injectable.getInjectableClass());
          }
        }
      }

      if(constructorCount < 1) {
        throw new BindingException("No suitable constructor found; provide an empty constructor or annotate one with @Inject: " + injectable.getInjectableClass());
      }
      else if(constructorCount > 1) {
        throw new BindingException("Multiple constructors found to be annotated with @Inject, but only one allowed: " + injectable.getInjectableClass());
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
  }

  @Override
  public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers, Map<AccessibleObject, Binding> bindings) {
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
