package hs.ddif.core.inject.consistency;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.Key;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.store.Resolver;
import hs.ddif.core.store.StoreConsistencyPolicy;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Policy that makes sure the InjectableStore at all times contains
 * injectables that can be fully resolved.
 *
 * @param <T> the type of {@link ResolvableInjectable} the policy uses
 */
public class InjectorStoreConsistencyPolicy<T extends ResolvableInjectable> implements StoreConsistencyPolicy<T> {

  /**
   * Map containing the number of times a specific Key (a reference to a specific class
   * with qualifiers) is referenced.
   */
  private final Map<Key, Integer> referenceCounters = new HashMap<>();

  /**
   * Set containing known scope annotations.
   */
  private final Set<Class<? extends Annotation>> knownScopeAnnotations = new HashSet<>();

  public InjectorStoreConsistencyPolicy(ScopeResolver... scopeResolvers) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      knownScopeAnnotations.add(scopeResolver.getScopeAnnotationClass());
    }
  }

  @Override
  public void addAll(Resolver<T> resolver, Collection<T> injectables) {

    /*
     * This is called when the given resolver is able to resolve all the new injectables.  If an exception is
     * thrown here, some clean up action may be needed at the caller.
     */

    // Check if scopes are known:
    for(T injectable : injectables) {
      ensureScopeIsKnown(injectable);
    }

    // First see if any new injectables would cause existing dependencies to become invalid:
    for(T injectable : injectables) {
      ensureSingularDependenciesHold(injectable, true);  // if this fails, just exit, no changes were made
    }

    // Check if the new injectables can have all their required dependencies resolved:
    for(T injectable : injectables) {
      ensureRequiredBindingsAreAvailable(resolver, injectable);
    }

    // Check if the new injectables have any circular references:
    ensureNoCyclicDependencies(resolver, injectables);

    /*
     * At this point, no modifications have been made to the policy data structures yet.  Below
     * updates will be made to the reference counters.  As all possible errors situations have
     * been checked, this should trigger no exceptions.
     */

    for(T injectable : injectables) {
      add(injectable);
    }
  }

  @Override
  public void removeAll(Resolver<T> baseResolver, Collection<T> injectables) {

    // Remove all injectables:
    for(T injectable : injectables) {
      remove(injectable);
    }

    /*
     * The injectables that were removed are now checked to see if their reference
     * counters are gone.  If any are not, all injectables are re-added and the
     * removal is cancelled by throwing an exception.
     */

    try {
      for(T injectable : injectables) {
        ensureSingularDependenciesHold(injectable, false);
      }
    }
    catch(Exception e) {
      // Given group of injectables was not valid to remove, re-add all and rethrow last exception:
      for(T injectable : injectables) {
        add(injectable);
      }

      throw e;
    }
  }

  private void add(T injectable) {
    increaseReferenceCounters(injectable, referenceCounters);
  }

  private void remove(T injectable) {
    decreaseReferenceCounters(injectable, referenceCounters, false);
  }

  private static void increaseReferenceCounters(ResolvableInjectable injectable, Map<Key, Integer> referenceCounters) {
    for(Binding binding : injectable.getBindings()) {
      Key requiredKey = binding.getRequiredKey();

      if(requiredKey != null) {
        addReference(requiredKey, referenceCounters);
      }
    }
  }

  private static void decreaseReferenceCounters(ResolvableInjectable injectable, Map<Key, Integer> referenceCounters, boolean allowNegativeReferenceCount) {
    for(Binding binding : injectable.getBindings()) {
      Key requiredKey = binding.getRequiredKey();

      if(requiredKey != null) {
        removeReference(requiredKey, referenceCounters, allowNegativeReferenceCount);
      }
    }
  }

  private static void addReference(Key key, Map<Key, Integer> referenceCounters) {
    referenceCounters.merge(key, 1, Integer::sum);
  }

  private static void removeReference(Key key, Map<Key, Integer> referenceCounters, boolean allowNegativeReferenceCount) {
    referenceCounters.merge(key, -1, (a, b) -> a + b == 0 ? null : a + b);

    if(referenceCounters.getOrDefault(key, 0) < 0 && !allowNegativeReferenceCount) {
      throw new AssertionError("reference counter became negative: " + key);
    }
  }

  /*
   * Checks if adding (or removing) the given type with qualifiers would cause any direct bindings dependent
   * on the type to become unresolvable.  The existence of the key in the referenceCounters map means there
   * is exactly one of the given type with qualifiers available already (reference counter is never 0, the key
   * is not present in that case).
   *
   * Therefore adding (or removing) a type which is assignable to one of the keys in the map (and has the
   * same qualifiers) would cause some bindings to become unresolvable, either because the dependency would
   * no longer be available, or because multiple options would become available.
   */
  // Note: loops through all bindings made, quite expensive when there are many keys due to generic assignment check
  private void ensureSingularDependenciesHold(Injectable injectable, boolean isAdd) {
    Type type = injectable.getType();
    Set<AnnotationDescriptor> qualifiers = injectable.getQualifiers();

    for(Key key : referenceCounters.keySet()) {
      if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
        throw new ViolatesSingularDependencyException(type, key, isAdd);
      }
    }
  }

  private void ensureRequiredBindingsAreAvailable(Resolver<T> resolver, T injectable) {

    /*
     * Check the created bindings for unresolved or ambiguous dependencies and scope problems:
     */

    for(Binding binding : injectable.getBindings()) {
      Key requiredKey = binding.getRequiredKey();

      if(requiredKey != null) {
        Set<T> injectables = resolver.resolve(requiredKey.getType(), (Object[])requiredKey.getQualifiersAsArray());

        ensureBindingIsSingular(binding, injectables);

        T dependency = injectables.iterator().next();  // Previous ensureBindingIsSingular check ensures there is only a single element in the set

        ensureBindingScopeIsValid(injectable, dependency);
      }
    }
  }

  private void ensureBindingIsSingular(Binding binding, Set<T> injectables) {
    if(injectables.size() != 1) {
      throw new UnresolvableDependencyException(binding, injectables);
    }
  }

  private static void ensureBindingScopeIsValid(ResolvableInjectable injectable, ResolvableInjectable dependentInjectable) {

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

  private void ensureScopeIsKnown(T injectable) {
    Annotation scope = injectable.getScope();

    if(scope != null && !knownScopeAnnotations.contains(scope.annotationType())) {
      throw new UnknownScopeException("Unknown scope " + scope + ": " + injectable);
    }
  }

  private void ensureNoCyclicDependencies(Resolver<T> resolver, Collection<T> injectables) {
    class CycleDetector {
      Set<T> input = new HashSet<>(injectables);
      Set<T> visited = new HashSet<>();
      List<T> visiting = new ArrayList<>();

      List<T> hasCycle() {
        for(T injectable : injectables) {
          if(!visited.contains(injectable) && hasCycle(injectable)) {
            return visiting;
          }
        }

        return visiting;
      }

      boolean hasCycle(T injectable) {
        visiting.add(injectable);

        for(Binding binding : injectable.getBindings()) {
          Key key = binding.getRequiredKey();

          if(key != null) {
            for(T boundInjectable : resolver.resolve(key.getType(), (Object[])key.getQualifiersAsArray())) {
              if(visiting.contains(boundInjectable)) {
                return true;
              }
              else if(!visited.contains(boundInjectable) && input.contains(boundInjectable) && hasCycle(boundInjectable)) {
                return true;
              }
            }
          }
        }

        visiting.remove(injectable);
        visited.add(injectable);

        return false;
      }
    }

    List<T> cycle = new CycleDetector().hasCycle();

    if(!cycle.isEmpty()) {
      throw new CyclicDependencyException(cycle);
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
