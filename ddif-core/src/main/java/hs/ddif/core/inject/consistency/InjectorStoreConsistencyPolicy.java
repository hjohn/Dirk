package hs.ddif.core.inject.consistency;

import hs.ddif.core.inject.instantiator.Binding;
import hs.ddif.core.inject.instantiator.ResolvableInjectable;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;
import hs.ddif.core.store.StoreConsistencyPolicy;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * Structure keeping track of {@link Key} used in direct bindings that must be available
   * for the binding to be satisfied. When the direct binding is required, then exactly
   * one source must be available to supply that binding. If the binding is optional, then
   * zero or one sources must be available.
   */
  private final Map<Class<?>, Map<Key, Node>> nodes = new HashMap<>();

  /**
   * Set containing known scope annotations.
   */
  private final Set<Class<? extends Annotation>> knownScopeAnnotations = new HashSet<>();

  /**
   * Constructs a new instance.
   *
   * @param scopeResolvers an array of {@link ScopeResolver}s, cannot be null
   */
  public InjectorStoreConsistencyPolicy(ScopeResolver... scopeResolvers) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      knownScopeAnnotations.add(scopeResolver.getScopeAnnotationClass());
    }
  }

  @Override
  public void addAll(Resolver<T> resolver, Collection<T> injectables) {

    // Check if scopes are known:
    for(T injectable : injectables) {
      ensureScopeIsKnown(injectable);
    }

    // Check if the new injectables can have all their required dependencies resolved:
    for(T injectable : injectables) {
      ensureRequiredBindingsAreAvailable(resolver, injectable);
    }

    ensureNoCyclicDependencies(resolver, injectables);

    addInjectables(resolver, injectables).ifPresent(violation -> {
      removeInjectables(injectables);
      violation.doThrow();
    });
  }

  @Override
  public void removeAll(Resolver<T> baseResolver, Collection<T> injectables) {
    removeInjectables(injectables).ifPresent(violation -> {
      addInjectables(baseResolver, injectables);
      violation.doThrow();
    });
  }

  private Optional<Violation> addInjectables(Resolver<T> resolver, Collection<T> injectables) {
    for(T injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        if(!binding.isCollection()) {
          Key key = binding.getKey();

          addTarget(resolver, key, binding.isOptional(), injectables);
        }
      }
    }

    return addSources(injectables);
  }

  private Optional<Violation> removeInjectables(Collection<T> injectables) {
    for(T injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        if(!binding.isCollection()) {
          Key key = binding.getKey();

          removeTarget(key, binding.isOptional());
        }
      }
    }

    return removeSources(injectables);
  }

  private static <T extends ResolvableInjectable> void ensureRequiredBindingsAreAvailable(Resolver<T> resolver, T injectable) {

    /*
     * Check the created bindings for unresolved or ambiguous dependencies and scope problems:
     */

    for(Binding binding : injectable.getBindings()) {
      if(!binding.isCollection()) {
        Key key = binding.getKey();
        Set<T> injectables = resolver.resolve(key);

        // The binding is a single binding, if there are more than one matches it is ambiguous, and if there is no match then it must be optional
        if(injectables.size() > 1 || (!binding.isOptional() && injectables.size() < 1)) {
          throw new UnresolvableDependencyException(binding, injectables);
        }

        // Check scope only for direct bindings. Indirect ones that inject a Provider can be used anywhere.
        if(binding.isDirect() && !injectables.isEmpty()) {
          T dependency = injectables.iterator().next();  // Previous check ensures there is only a single element in the set

          ensureBindingScopeIsValid(injectable, dependency);
        }
      }
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

  private static <T extends ResolvableInjectable> void ensureNoCyclicDependencies(Resolver<T> resolver, Collection<T> injectables) {
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
          if(!binding.isCollection() && binding.isDirect()) {
            Key key = binding.getKey();

            for(T boundInjectable : resolver.resolve(key)) {
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

  void checkInvariants() {
    for(Map<Key, Node> map : nodes.values()) {
      for(Node node : map.values()) {
        if(node.isInvalid()) {
          throw new IllegalStateException(node.toString());
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

  /**
   * Adds the given sources to this policy. If adding the given sources would cause
   * any targets to become unresolvable a {@link Violation} will be returned.<p>
   *
   * Implementation note: this method modifies the data structures while detecting violations
   * at the same time. The data structures are NOT rolled back when a violation is detected,
   * but are left in a state where all changes have been applied. This means the change can
   * be completely undone by calling {@link #removeSources(Collection)} with the same sources.
   *
   * @param sources a collection of sources to add, cannot be null
   * @return an optional {@link Violation} if one was detected, never null
   */
  private Optional<Violation> addSources(Collection<T> sources) {
    Violation violation = null;

    for(T source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(TypeUtils.getRawType(type, null))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = nodesByKeys.get(key);

              node.increaseSources();

              if(violation == null && node.isInvalid()) {
                violation = new Violation(type, key, true);
              }
            }
          }
        }
      }
    }

    return Optional.ofNullable(violation);
  }

  /**
   * Removes the given sources from this policy. If removing the given sources would cause
   * any targets to become unresolvable a {@link Violation} will be returned.<p>
   *
   * Implementation note: this method modifies the data structures while detecting violations
   * at the same time. The data structures are NOT rolled back when a violation is detected,
   * but are left in a state where all changes have been applied. This means the change can
   * be completely undone by calling {@link #addSources(Collection)} with the same sources.
   *
   * @param sources a collection of sources to remove, cannot be null
   * @return an optional {@link Violation} if one was detected, never null
   */
  private Optional<Violation> removeSources(Collection<T> sources) {
    Violation violation = null;

    for(T source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(TypeUtils.getRawType(type, null))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = nodesByKeys.get(key);

              node.decreaseSources();

              if(violation == null && node.isInvalid()) {
                violation = new Violation(type, key, false);
              }
            }
          }
        }
      }
    }

    return Optional.ofNullable(violation);
  }

  private void addTarget(Resolver<T> resolver, Key key, boolean isOptional, Collection<T> sources) {
    Class<?> cls = TypeUtils.getRawType(key.getType(), null);

    nodes.computeIfAbsent(cls, k -> new HashMap<>())
      .computeIfAbsent(key, k -> {
        // when a new Key is added, initialise the Node with the current number of candidates that can satisfy it
        Set<T> candidates = resolver.resolve(key);
        Node node = new Node(candidates.size());

        for(T source : sources) {
          if(candidates.contains(source)) {
            node.decreaseSources();  // exclude candidates that are new; they will get counted when calling #addSources
          }
        }

        return node;
      })
      .increaseTargets(isOptional);
  }

  private void removeTarget(Key key, boolean isOptional) {
    Class<?> cls = TypeUtils.getRawType(key.getType(), null);

    nodes.computeIfPresent(
      cls,
      (c, m) -> m.computeIfPresent(key, (k, n) -> n.decreaseTargets(isOptional) ? null : n) == null ? null : m
    );
  }

  static class Violation {
    private final Type type;
    private final Key key;
    private final boolean isAdd;

    Violation(Type type, Key key, boolean isAdd) {
      this.type = type;
      this.key = key;
      this.isAdd = isAdd;
    }

    void doThrow() {
      throw new ViolatesSingularDependencyException(type, key, isAdd);
    }
  }

  static class Node {

    /**
     * The number of targets that require at least one source.
     */
    int greaterThanZeroReferences;

    /**
     * The number of targets that require at most one source.
     */
    int lessThanTwoReferences;

    /**
     * The number of sources available.
     */
    int sourceCount;

    Node(int sourceCount) {
      this.sourceCount = sourceCount;
    }

    boolean increaseTargets(boolean optional) {
      greaterThanZeroReferences += optional ? 0 : 1;
      lessThanTwoReferences++;

      return greaterThanZeroReferences == 0 && lessThanTwoReferences == 0;
    }

    boolean decreaseTargets(boolean optional) {
      greaterThanZeroReferences -= optional ? 0 : 1;
      lessThanTwoReferences--;

      return greaterThanZeroReferences == 0 && lessThanTwoReferences == 0;
    }

    void increaseSources() {
      sourceCount++;
    }

    void decreaseSources() {
      sourceCount--;
    }

    boolean isInvalid() {
      return (greaterThanZeroReferences > 0 && sourceCount < 1) || (lessThanTwoReferences > 0 && sourceCount > 1);
    }

    @Override
    public String toString() {
      return "Node[>0 = " + greaterThanZeroReferences + "; <2 = " + lessThanTwoReferences + "; sourceCount = " + sourceCount + "]";
    }
  }
}
