package hs.ddif.core.inject.consistency;

import hs.ddif.core.bind.Binding;
import hs.ddif.core.bind.Key;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.StoreConsistencyPolicy;
import hs.ddif.core.util.AnnotationDescriptor;

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
public class InjectorStoreConsistencyPolicy<T extends ScopedInjectable> implements StoreConsistencyPolicy<T> {

  /**
   * Map containing the number of times a specific Key (a reference to a specific class
   * with qualifiers) is referenced.
   */
  private final Map<Key, Integer> referenceCounters = new HashMap<>();

  @Override
  public void checkAddition(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers) {
    ensureSingularDependenciesHold(injectable.getType(), qualifiers);

    Map<AccessibleObject, Binding[]> bindings = injectable.getBindings();

    /*
     * Check the created bindings for unresolved or ambigious dependencies and scope problems:
     */

    for(Map.Entry<AccessibleObject, Binding[]> entry : bindings.entrySet()) {
      for(Binding binding : entry.getValue()) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          Set<T> injectables = injectableStore.resolve(requiredKey.getType(), (Object[])requiredKey.getQualifiersAsArray());

          ensureBindingIsSingular(injectable, entry.getKey(), requiredKey, injectables);

          if(!binding.isProvider()) {  // When wrapped in a Provider, there are never any scope conflicts
            ensureBindingScopeIsValid(injectable, injectables.iterator().next());  // Previous check ensures there is only a single element in the set
          }
        }
      }
    }
  }

  @Override
  public void checkRemoval(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers) {
    ensureSingularDependenciesHold(injectable.getType(), qualifiers);
  }

  @Override
  public void add(T injectable) {
    for(Binding[] bindings : injectable.getBindings().values()) {
      for(Binding binding : bindings) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          addReference(requiredKey);
        }
      }
    }
  }

  @Override
  public void remove(T injectable) {
    for(Binding[] bindings : injectable.getBindings().values()) {
      for(Binding binding : bindings) {
        Key requiredKey = binding.getRequiredKey();

        if(requiredKey != null) {
          removeReference(requiredKey);
        }
      }
    }
  }

  private void addReference(Key key) {
    Integer referenceCounter = referenceCounters.get(key);

    if(referenceCounter == null) {
      referenceCounter = 1;
    }
    else {
      referenceCounter++;
    }

    referenceCounters.put(key, referenceCounter);
  }

  private void removeReference(Key key) {
    Integer referenceCounter = referenceCounters.remove(key);

    if(referenceCounter == null) {
      throw new IllegalStateException("Assertion error");
    }

    referenceCounter--;

    if(referenceCounter > 0) {
      referenceCounters.put(key, referenceCounter);
    }
  }

  private void ensureSingularDependenciesHold(Type type, Set<AnnotationDescriptor> qualifiers) {
    for(Key key : referenceCounters.keySet()) {
      if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
        throw new ViolatesSingularDependencyException(type, key, true);
      }
    }
  }

  private void ensureBindingIsSingular(T injectable, AccessibleObject accessibleObject, Key requiredKey, Set<T> injectables) {
    if(injectables.size() != 1) {
      throw new UnresolvableDependencyException(injectable, accessibleObject, requiredKey, injectables);
    }
  }

  private static void ensureBindingScopeIsValid(ScopedInjectable injectable, ScopedInjectable dependentInjectable) {

    /*
     * Perform scope check.  Having a dependency on a narrower scoped injectable would mean the injected
     * dependency of narrower scope is not updated when the scope changes, resulting in unpredictable
     * behaviour.
     *
     * Other frameworks solve this by injecting an adapter instead that relays calls to a specific instance
     * of the dependency based on current scope.  As this is non-trivial a ScopeConflictException is
     * thrown instead.
     */

    Annotation dependencyScopeAnnotation = dependentInjectable.getScope();
    Annotation injectableScopeAnnotation = injectable.getScope();

    if(isNarrowerScope(injectableScopeAnnotation, dependencyScopeAnnotation)) {
      throw new ScopeConflictException(injectable + " is dependent on narrower scoped dependency: " + dependentInjectable.getType());
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
}
