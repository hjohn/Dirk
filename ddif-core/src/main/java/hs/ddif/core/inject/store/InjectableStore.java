package hs.ddif.core.inject.store;

import hs.ddif.api.util.Types;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.ExtendedScopeResolver;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.store.QualifiedTypeStore;
import hs.ddif.core.store.Resolver;
import hs.ddif.spi.config.ProxyStrategy;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.TypeTrait;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A store for {@link Injectable}s which ensures that it at all times contains
 * only injectables that can be fully resolved.
 */
public class InjectableStore implements Resolver<Injectable<?>> {
  private final BindingManager bindingManager;

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
  private final QualifiedTypeStore<Injectable<?>> qualifiedTypeStore;

  private final ProxyStrategy proxyStrategy;

  /**
   * Constructs a new instance.
   *
   * @param bindingManager an {@link BindingManager}, cannot be {@code null}
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   */
  public InjectableStore(BindingManager bindingManager, ProxyStrategy proxyStrategy) {
    this.bindingManager = Objects.requireNonNull(bindingManager, "bindingManager");
    this.proxyStrategy = Objects.requireNonNull(proxyStrategy, "proxyStrategy");
    this.qualifiedTypeStore = new QualifiedTypeStore<>(i -> new Key(i.getType(), i.getQualifiers()), i -> i.getTypes());
  }

  @Override
  public Set<Injectable<?>> resolve(Key key) {
    Map<Key, Node> map = nodes.get(Types.raw(key.getType()));

    if(map != null) {
      Node node = map.get(key);

      if(node != null) {
        return new HashSet<>(node.sources);
      }
    }

    return qualifiedTypeStore.resolve(key);
  }

  /**
   * Checks if there is an {@link Injectable} associated with the given {@link Key} in the store.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @return {@code true} if there was an {@link Injectable} associated with the given {@link Key},
   *   otherwise {@code false}
   */
  public boolean contains(Key key) {
    return qualifiedTypeStore.contains(key);
  }

  /**
   * Adds multiple {@link Injectable}s to the store. If this method throws an exception then
   * the store will be unmodified.
   *
   * @param injectables a collection of {@link Injectable}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public synchronized void putAll(Collection<Injectable<?>> injectables) {
    qualifiedTypeStore.putAll(injectables);

    try {
      addBindings(injectables);  // modifies structure but must be done before checking for required bindings

      try {
        ensureNoCyclicDependencies(injectables);

        // Check if the new injectables can have all their required dependencies resolved:
        for(Injectable<?> injectable : injectables) {
          ensureRequiredBindingsAreAvailable(injectable);
        }

        addInjectables(injectables).ifPresent(violation -> {
          removeInjectables(injectables);
          violation.doThrow();
        });
      }
      catch(Exception e) {
        removeBindings(injectables);

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
  public synchronized void removeAll(Collection<Injectable<?>> injectables) {
    qualifiedTypeStore.removeAll(injectables);

    try {
      removeInjectables(injectables).ifPresent(violation -> {
        addInjectables(injectables);
        violation.doThrow();
      });

      removeBindings(injectables);
      removeScopedInstances(injectables);
    }
    catch(Exception e) {
      qualifiedTypeStore.putAll(injectables);

      throw e;
    }
  }

  private void addBindings(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        bindingManager.addBinding(binding);
      }
    }
  }

  private void removeBindings(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        bindingManager.removeBinding(binding);
      }
    }
  }

  private static void removeScopedInstances(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      injectable.getScopeResolver().remove(injectable);
    }
  }

  private Optional<Violation> addInjectables(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Set<TypeTrait> typeTraits = bindingManager.getTypeTraits(binding);
        Key key = bindingManager.getSearchKey(binding);

        addTarget(key, typeTraits.contains(TypeTrait.REQUIRES_AT_LEAST_ONE), typeTraits.contains(TypeTrait.REQUIRES_AT_MOST_ONE), injectables);
      }
    }

    return addSources(injectables);
  }

  private Optional<Violation> removeInjectables(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        Set<TypeTrait> typeTraits = bindingManager.getTypeTraits(binding);
        Key key = bindingManager.getSearchKey(binding);

        removeTarget(key, typeTraits.contains(TypeTrait.REQUIRES_AT_LEAST_ONE), typeTraits.contains(TypeTrait.REQUIRES_AT_MOST_ONE));
      }
    }

    return removeSources(injectables);
  }

  private void ensureRequiredBindingsAreAvailable(Injectable<?> injectable) {

    /*
     * Check the created bindings for unresolved or ambiguous dependencies and scope problems:
     */

    for(Binding binding : injectable.getBindings()) {
      Set<TypeTrait> typeTraits = bindingManager.getTypeTraits(binding);

      if(typeTraits.contains(TypeTrait.REQUIRES_AT_MOST_ONE)) {
        Key key = bindingManager.getSearchKey(binding);
        Set<Injectable<?>> injectables = qualifiedTypeStore.resolve(key);

        // The binding is a single binding, if there are more than one matches it is ambiguous, and if there is no match then it must be optional
        if(injectables.size() > 1 || (typeTraits.contains(TypeTrait.REQUIRES_AT_LEAST_ONE) && injectables.size() < 1)) {
          throw new UnresolvableDependencyException(key, binding, injectables);
        }

        // Check scope only for non lazy bindings. Lazy ones that inject a Provider can be used anywhere.
        if(!typeTraits.contains(TypeTrait.LAZY) && !injectables.isEmpty()) {
          Injectable<?> dependency = injectables.iterator().next();  // Previous check ensures there is only a single element in the set

          ensureBindingScopeIsValid(injectable, dependency);
        }
      }
    }
  }

  private void ensureBindingScopeIsValid(Injectable<?> injectable, Injectable<?> dependentInjectable) {

    /*
     * Perform scope check.
     *
     * The dependent injectable is injected into the given injectable.
     *
     * When the dependent injectable is a pseudo-scope or is proxyable, there is never any conflict.
     * When both injectables have the same scope, there is never any conflict.
     *
     * The ProxyStrategy is responsible for creating proxies, and may not create any at all. It is 
     * however entirely possible to use normal scopes (non pseudo-scopes) without proxies by making 
     * use of an indirect form of injection like that which providers offer.
     *
     * Note that restrictions may apply to types in order to wrap them with a proxy. The early call
     * here to create the proxy ensures that a proxy will be available later. Failing early here
     * prevents the store from going into a state where unrelated objects may fail to be created
     * because a dependent proxy cannot be created.
     */

    ExtendedScopeResolver dependentScopeResolver = dependentInjectable.getScopeResolver();
    ExtendedScopeResolver injectableScopeResolver = injectable.getScopeResolver();

    boolean needsProxy = !dependentScopeResolver.isPseudoScope() && dependentScopeResolver.getAnnotationClass() != injectableScopeResolver.getAnnotationClass();

    if(needsProxy) {
      try {
        proxyStrategy.createProxy(Types.raw(dependentInjectable.getType()));
      }
      catch(Exception e) {
        throw new ScopeConflictException("Type [" + injectable.getType() + "] with scope [" + injectableScopeResolver.getAnnotationClass() + "] is dependent on [" + dependentInjectable.getType() + "] with normal scope [" + dependentScopeResolver.getAnnotationClass() + "]; this requires the use of a provider or proxy", e);
      }
    }
  }

  private void ensureNoCyclicDependencies(Collection<Injectable<?>> injectables) {
    class CycleDetector {
      Set<Injectable<?>> input = new HashSet<>(injectables);
      Set<Injectable<?>> visited = new HashSet<>();
      List<Injectable<?>> visiting = new ArrayList<>();

      List<Injectable<?>> hasCycle() {
        for(Injectable<?> injectable : injectables) {
          if(!visited.contains(injectable) && hasCycle(injectable)) {
            return visiting;
          }
        }

        return visiting;
      }

      boolean hasCycle(Injectable<?> injectable) {
        visiting.add(injectable);

        for(Binding binding : injectable.getBindings()) {
          Set<TypeTrait> typeTraits = bindingManager.getTypeTraits(binding);

          if(!typeTraits.contains(TypeTrait.LAZY)) {
            Key key = binding.getKey();

            for(Injectable<?> boundInjectable : qualifiedTypeStore.resolve(key)) {
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

    List<Injectable<?>> cycle = new CycleDetector().hasCycle();

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
  private Optional<Violation> addSources(Collection<Injectable<?>> sources) {
    Violation violation = null;

    for(Injectable<?> source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(Types.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
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
  private Optional<Violation> removeSources(Collection<Injectable<?>> sources) {
    Violation violation = null;

    for(Injectable<?> source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Key key : nodesByKeys.keySet()) {
            if(Types.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
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

  private void addTarget(Key key, boolean minimumOne, boolean maximumOne, Collection<Injectable<?>> sources) {
    Class<?> cls = Types.raw(key.getType());

    nodes.computeIfAbsent(cls, k -> new HashMap<>())
      .computeIfAbsent(key, k -> {
        // when a new Key is added, initialise the Node with the current number of candidates that can satisfy it
        Set<Injectable<?>> candidates = qualifiedTypeStore.resolve(key);
        Node node = new Node(candidates);

        for(Injectable<?> source : sources) {
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
    final Set<Injectable<?>> sources;

    Node(Set<Injectable<?>> sources) {
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

    void increaseSources(Injectable<?> source) {
      if(!sources.add(source)) {
        throw new AssertionError("Node already contained source: " + source);
      }
    }

    void decreaseSources(Injectable<?> source) {
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
