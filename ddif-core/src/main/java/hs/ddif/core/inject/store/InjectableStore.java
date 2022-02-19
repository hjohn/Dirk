package hs.ddif.core.inject.store;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.store.Resolver;
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
 * A store for {@link Injectable}s which ensures that it at all times contains
 * only injectables that can be fully resolved.
 */
public class InjectableStore implements Resolver<Injectable> {
  private final InstantiatorBindingMap instantiatorBindingMap;
  private final ScopeResolverManager scopeResolverManager;

  /**
   * Structure keeping track of {@link Key}s used in bindings that must be available
   * for the binding to be satisfied.
   *
   * <p>The structure only contains each {@link Key} once, grouped by its (raw) class for
   * improved performance when updating the structure. A search for a key will however
   * always yield exactly one match.
   *
   * <p>The groups used can be anything, as long as a group can be deterministically
   * determined from a given key. Preferably the group chosen is as distinct as possible.
   * It would for example be possible to choose an annotation, like the Named annotation,
   * as a group.
   */
  private final Map<Class<?>, Map<Key, Node>> nodes = new HashMap<>();

  /**
   * Underlying store to which calls are delegated.
   */
  private final QualifiedTypeStore<Injectable> qualifiedTypeStore = new QualifiedTypeStore<>();

  /**
   * Constructs a new instance.
   *
   * @param instantiatorBindingMap an {@link InstantiatorBindingMap}, cannot be {@code null}
   * @param scopeResolverManager a {@link ScopeResolverManager}, cannot be {@code null}
   */
  public InjectableStore(InstantiatorBindingMap instantiatorBindingMap, ScopeResolverManager scopeResolverManager) {
    this.instantiatorBindingMap = instantiatorBindingMap;
    this.scopeResolverManager = scopeResolverManager;
  }

  @Override
  public Set<Injectable> resolve(Key key) {
    Map<Key, Node> map = nodes.get(Types.raw(key.getType()));

    if(map != null) {
      Node node = map.get(key);

      if(node != null) {
        return new HashSet<>(node.sources);
      }
    }

    return qualifiedTypeStore.resolve(key);
  }

  public boolean contains(Key key) {
    return qualifiedTypeStore.contains(key);
  }

  /**
   * Adds multiple {@link Injectable}s to the store. If this method throws an exception then
   * the store will be unmodified.
   *
   * @param injectables a collection of {@link Injectable}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public synchronized void putAll(Collection<Injectable> injectables) {
    qualifiedTypeStore.putAll(injectables);

    try {
      // Check if scopes are known:
      for(Injectable injectable : injectables) {
        ensureScopeIsKnown(injectable);
      }

      addInstanceFactories(injectables);  // modifies structure but must be done before checking for required bindings

      try {
        ensureNoCyclicDependencies(injectables);

        // Check if the new injectables can have all their required dependencies resolved:
        for(Injectable injectable : injectables) {
          ensureRequiredBindingsAreAvailable(injectable);
        }

        addInjectables(injectables).ifPresent(violation -> {
          removeInjectables(injectables);
          violation.doThrow();
        });
      }
      catch(Exception e) {
        removeInstanceFactories(injectables);

        throw e;
      }
    }
    catch(Exception e) {
      qualifiedTypeStore.removeAll(injectables);

      throw e;
    }
  }

  /**
   * Removes multiple {@link Injectable}s from the store. If this method throws an exception then
   * the store will be unmodified.
   *
   * @param injectables a collection of {@link Injectable}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public synchronized void removeAll(Collection<Injectable> injectables) {
    qualifiedTypeStore.removeAll(injectables);

    try {
      removeInjectables(injectables).ifPresent(violation -> {
        addInjectables(injectables);
        violation.doThrow();
      });

      removeInstanceFactories(injectables);
      removeScopedInstances(injectables);
    }
    catch(Exception e) {
      qualifiedTypeStore.putAll(injectables);

      throw e;
    }
  }

  private void addInstanceFactories(Collection<Injectable> injectables) {
    for(Injectable injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        instantiatorBindingMap.addBinding(binding);
      }
    }
  }

  private void removeInstanceFactories(Collection<Injectable> injectables) {
    for(Injectable injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        instantiatorBindingMap.removeBinding(binding);
      }
    }
  }

  private void removeScopedInstances(Collection<Injectable> injectables) {
    for(Injectable injectable : injectables) {
      ScopeResolver scopeResolver = scopeResolverManager.getScopeResolver(injectable.getScope());

      scopeResolver.remove(injectable);
    }
  }

  private Optional<Violation> addInjectables(Collection<Injectable> injectables) {
    for(Injectable injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Instantiator<?> instantiator = instantiatorBindingMap.getInstantiator(binding);
        Key key = instantiator.getKey();

        addTarget(key, instantiator.requiresAtLeastOne(), instantiator.requiresAtMostOne(), injectables);
      }
    }

    return addSources(injectables);
  }

  private Optional<Violation> removeInjectables(Collection<Injectable> injectables) {
    for(Injectable injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Instantiator<?> instantiator = instantiatorBindingMap.getInstantiator(binding);
        Key key = instantiator.getKey();

        removeTarget(key, instantiator.requiresAtLeastOne(), instantiator.requiresAtMostOne());
      }
    }

    return removeSources(injectables);
  }

  private void ensureRequiredBindingsAreAvailable(Injectable injectable) {

    /*
     * Check the created bindings for unresolved or ambiguous dependencies and scope problems:
     */

    for(Binding binding : injectable.getBindings()) {
      Instantiator<?> instantiator = instantiatorBindingMap.getInstantiator(binding);
      boolean requiresAtLeastOne = instantiator.requiresAtLeastOne();
      boolean requiresAtMostOne = instantiator.requiresAtMostOne();

      if(requiresAtMostOne) {
        Key key = instantiator.getKey();
        Set<Injectable> injectables = qualifiedTypeStore.resolve(key);

        // The binding is a single binding, if there are more than one matches it is ambiguous, and if there is no match then it must be optional
        if(injectables.size() > 1 || (requiresAtLeastOne && injectables.size() < 1)) {
          throw new UnresolvableDependencyException(key, binding, injectables);
        }

        // Check scope only for non lazy bindings. Lazy ones that inject a Provider can be used anywhere.
        if(!instantiator.isLazy() && !injectables.isEmpty()) {
          Injectable dependency = injectables.iterator().next();  // Previous check ensures there is only a single element in the set

          ensureBindingScopeIsValid(injectable, dependency);
        }
      }
    }
  }

  private static void ensureBindingScopeIsValid(Injectable injectable, Injectable dependentInjectable) {

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

  private void ensureScopeIsKnown(Injectable injectable) {
    Annotation scope = injectable.getScope();

    if(scope != null && !scopeResolverManager.isRegisteredScope(scope.annotationType())) {
      throw new UnknownScopeException("Unknown scope " + scope + ": " + injectable);
    }
  }

  private void ensureNoCyclicDependencies(Collection<Injectable> injectables) {
    class CycleDetector {
      Set<Injectable> input = new HashSet<>(injectables);
      Set<Injectable> visited = new HashSet<>();
      List<Injectable> visiting = new ArrayList<>();

      List<Injectable> hasCycle() {
        for(Injectable injectable : injectables) {
          if(!visited.contains(injectable) && hasCycle(injectable)) {
            return visiting;
          }
        }

        return visiting;
      }

      boolean hasCycle(Injectable injectable) {
        visiting.add(injectable);

        for(Binding binding : injectable.getBindings()) {
          Instantiator<?> instantiator = instantiatorBindingMap.getInstantiator(binding);

          if(!instantiator.isLazy()) {
            Key key = binding.getKey();

            for(Injectable boundInjectable : qualifiedTypeStore.resolve(key)) {
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

    List<Injectable> cycle = new CycleDetector().hasCycle();

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
   * @param sources a collection of sources to add, cannot be {@code null}
   * @return an optional {@link Violation} if one was detected, never {@code null}
   */
  private Optional<Violation> addSources(Collection<Injectable> sources) {
    Violation violation = null;

    for(Injectable source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = nodesByKeys.get(key);

              node.increaseSources(source);

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
   * @param sources a collection of sources to remove, cannot be {@code null}
   * @return an optional {@link Violation} if one was detected, never {@code null}
   */
  private Optional<Violation> removeSources(Collection<Injectable> sources) {
    Violation violation = null;

    for(Injectable source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(TypeUtils.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = nodesByKeys.get(key);

              node.decreaseSources(source);

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

  private void addTarget(Key key, boolean minimumOne, boolean maximumOne, Collection<Injectable> sources) {
    Class<?> cls = Types.raw(key.getType());

    nodes.computeIfAbsent(cls, k -> new HashMap<>())
      .computeIfAbsent(key, k -> {
        // when a new Key is added, initialise the Node with the current number of candidates that can satisfy it
        Set<Injectable> candidates = qualifiedTypeStore.resolve(key);
        Node node = new Node(candidates);

        for(Injectable source : sources) {
          if(candidates.contains(source)) {
            node.decreaseSources(source);  // exclude candidates that are new; they will get counted when calling #addSources
          }
        }

        return node;
      })
      .increaseTargets(minimumOne, maximumOne);
  }

  private void removeTarget(Key key, boolean minimumOne, boolean maximumOne) {
    Class<?> cls = Types.raw(key.getType());

    nodes.computeIfPresent(
      cls,
      (c, m) -> m.computeIfPresent(key, (k, n) -> n.decreaseTargets(minimumOne, maximumOne) ? null : n) == null ? null : m
    );
  }

  private static class Violation {
    final Type type;
    final Key key;
    final boolean isAdd;

    Violation(Type type, Key key, boolean isAdd) {
      this.type = type;
      this.key = key;
      this.isAdd = isAdd;
    }

    void doThrow() {
      throw new ViolatesSingularDependencyException(type, key, isAdd);
    }
  }

  private static class Node {

    /**
     * The number of targets that require at least one source.
     */
    int minimumOneReferences;

    /**
     * The number of targets that require at most one source.
     */
    int maximumOneReferences;

    /**
     * Total number of times these sources are referred.
     */
    int references;

    /**
     * The sources available.
     */
    final Set<Injectable> sources;

    Node(Set<Injectable> sources) {
      this.sources = new HashSet<>(sources);
    }

    boolean increaseTargets(boolean minimumOne, boolean maximumOne) {
      minimumOneReferences += minimumOne ? 1 : 0;
      maximumOneReferences += maximumOne ? 1 : 0;
      references++;

      return references == 0;
    }

    boolean decreaseTargets(boolean minimumOne, boolean maximumOne) {
      minimumOneReferences -= minimumOne ? 1 : 0;
      maximumOneReferences -= maximumOne ? 1 : 0;
      references--;

      return references == 0;
    }

    void increaseSources(Injectable source) {
      if(!sources.add(source)) {
        throw new AssertionError("Node already contained source: " + source);
      }
    }

    void decreaseSources(Injectable source) {
      if(!sources.remove(source)) {
        throw new AssertionError("Node did not contain source: " + source + "; available: " + sources);
      }
    }

    boolean isInvalid() {
      return (minimumOneReferences > 0 && sources.size() < 1) || (maximumOneReferences > 0 && sources.size() > 1) || references == 0;
    }

    @Override
    public String toString() {
      return "Node[>0 = " + minimumOneReferences + "; <2 = " + maximumOneReferences + "; references = " + references + "; sources = " + sources + "]";
    }
  }
}
