package hs.ddif.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Policy that makes sure the InjectableStore at all times contains
 * injectables that can be fully resolved.
 */
public class InjectorStoreConsistencyPolicy implements StoreConsistencyPolicy<Injectable> {

  /**
   * Map containing the number of times a specific Key (a reference to a specific class
   * with qualifiers) is referenced.
   */
  private final Map<Key, Integer> referenceCounters = new HashMap<>();

  @Override
  public void checkAddition(InjectableStore<Injectable> injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);

    Map<AccessibleObject, Binding[]> bindings = injectable.getBindings();

    /*
     * Check the created bindings for unresolved or ambigious dependencies:
     */

    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      for(Binding binding : entry.getValue()) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          Set<Injectable> injectables = injectableStore.resolve(requiredKey);

          if(injectables.isEmpty()) {
            throw new UnresolvedDependencyException(injectable, entry.getKey(), requiredKey);
          }
          if(injectables.size() > 1) {
            throw new AmbigiousDependencyException(injectable.getInjectableClass(), requiredKey, injectables);
          }

          /*
           * Perform scope check.  Having a dependency on a narrower scoped injectable would mean the injected
           * dependency of narrower scope is not updated when the scope changes, resulting in unpredictable
           * behaviour.
           *
           * Other frameworks solve this by injecting an adapter instead that relays calls to a specific instance
           * of the dependency based on current scope.  As this is non-trivial a ScopeConflictException is
           * thrown instead.
           */

          Injectable dependentInjectable = injectables.iterator().next();  // Previous checks ensure there is only a single element in the set

          if(!binding.isProvider()) {  // When wrapped in a Provider, there are never any scope conflicts
            Annotation dependencyScopeAnnotation = dependentInjectable.getScope();
            Annotation injectableScopeAnnotation = injectable.getScope();

            if(isNarrowerScope(injectableScopeAnnotation, dependencyScopeAnnotation)) {
              throw new ScopeConflictException(injectable + " is dependent on narrower scoped dependency: " + dependentInjectable.getInjectableClass());
            }
          }
        }
      }
    }
  }

  private static boolean isNarrowerScope(Annotation scope, Annotation dependencyScope) {
    if(scope == null) {
      return false;
    }

    if(dependencyScope == null) {
      return true;
    }

    return !dependencyScope.annotationType().equals(Singleton.class) && !scope.annotationType().equals(dependencyScope.annotationType());
  }

  @Override
  public void checkRemoval(InjectableStore<Injectable> injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
    ensureSingularDependenciesHold(injectable.getInjectableClass(), qualifiers);
  }

  void addReference(Key key) {
    Integer referenceCounter = referenceCounters.get(key);

    if(referenceCounter == null) {
      referenceCounter = 1;
    }
    else {
      referenceCounter++;
    }

    referenceCounters.put(key, referenceCounter);
  }

  void removeReference(Key key) {
    Integer referenceCounter = referenceCounters.remove(key);

    if(referenceCounter == null) {
      throw new RuntimeException("Assertion error");
    }

    referenceCounter--;

    if(referenceCounter > 0) {
      referenceCounters.put(key, referenceCounter);
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
