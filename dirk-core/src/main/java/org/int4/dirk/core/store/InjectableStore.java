package org.int4.dirk.core.store;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.int4.dirk.api.definition.AmbiguousDependencyException;
import org.int4.dirk.api.definition.AmbiguousRequiredDependencyException;
import org.int4.dirk.api.definition.CyclicDependencyException;
import org.int4.dirk.api.definition.DependencyException;
import org.int4.dirk.api.definition.ScopeConflictException;
import org.int4.dirk.api.definition.UnsatisfiedDependencyException;
import org.int4.dirk.api.definition.UnsatisfiedRequiredDependencyException;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.Key;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Types;

/**
 * A store for {@link Injectable}s which ensures that it at all times contains
 * only injectables that can be fully resolved.
 */
public class InjectableStore implements Resolver<Injectable<?>> {

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
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   */
  public InjectableStore(ProxyStrategy proxyStrategy) {
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
   * @throws DependencyException when adding an injectable would violate store rules
   */
  public synchronized void putAll(Collection<Injectable<?>> injectables) throws DependencyException {
    qualifiedTypeStore.putAll(injectables);

    try {
      ensureNoCyclicDependencies(injectables);

      // Check if the new injectables can have all their required dependencies resolved:
      for(Injectable<?> injectable : injectables) {
        ensureRequiredBindingsAreAvailable(injectable);
      }

      RegistrationViolation violation = addInjectables(injectables);

      if(violation != null) {
        removeInjectables(injectables);
        violation.doThrow();
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
   * @throws DependencyException when adding an injectable would violate store rules
   */
  public synchronized void removeAll(Collection<Injectable<?>> injectables) throws DependencyException {
    qualifiedTypeStore.removeAll(injectables);

    try {
      RemoveViolation violation = removeInjectables(injectables);

      if(violation != null) {
        addInjectables(injectables);
        violation.doThrow();
      }

      removeScopedInstances(injectables);
    }
    catch(Exception e) {
      qualifiedTypeStore.putAll(injectables);

      throw e;
    }
  }

  private static void removeScopedInstances(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      injectable.getScopeResolver().remove(injectable);
    }
  }

  private RegistrationViolation addInjectables(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        addTarget(binding.getElementKey(), !binding.isOptional() && binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_LEAST_ONE), binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_MOST_ONE), injectables);
      }
    }

    return addSources(injectables);
  }

  private RemoveViolation removeInjectables(Collection<Injectable<?>> injectables) {
    for(Injectable<?> injectable : injectables) {
      for(Binding binding : injectable.getBindings()) {
        removeTarget(binding.getElementKey(), !binding.isOptional() && binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_LEAST_ONE), binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_MOST_ONE));
      }
    }

    return removeSources(injectables);
  }

  private void ensureRequiredBindingsAreAvailable(Injectable<?> injectable) throws AmbiguousDependencyException, UnsatisfiedDependencyException, ScopeConflictException {

    /*
     * Check the created bindings for unresolved or ambiguous dependencies and scope problems:
     */

    for(Binding binding : injectable.getBindings()) {
      if(binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_MOST_ONE)) {
        Key elementKey = binding.getElementKey();
        Set<Injectable<?>> injectables = qualifiedTypeStore.resolve(elementKey);

        // The binding is a single binding, if there are more than one matches it is ambiguous, and if there is no match then it must be optional
        if(injectables.size() > 1) {
          throw new AmbiguousDependencyException("Multiple candidates for dependency [" + elementKey + "] required for " + binding + ": " + injectables);
        }
        if(!binding.isOptional() && binding.getTypeTraits().contains(TypeTrait.REQUIRES_AT_LEAST_ONE) && injectables.size() < 1) {
          throw new UnsatisfiedDependencyException("Missing dependency [" + elementKey + "] required for " + binding);
        }

        // Check scope only for non lazy bindings. Lazy ones that inject a Provider can be used anywhere.
        if(!binding.getTypeTraits().contains(TypeTrait.LAZY) && !injectables.isEmpty()) {
          Injectable<?> dependency = injectables.iterator().next();  // Previous check ensures there is only a single element in the set

          ensureBindingScopeIsValid(injectable, dependency);
        }
      }
    }
  }

  private void ensureBindingScopeIsValid(Injectable<?> injectable, Injectable<?> dependentInjectable) throws ScopeConflictException {

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

    boolean needsProxy = !dependentScopeResolver.isPseudoScope() && !dependentScopeResolver.getAnnotation().equals(injectableScopeResolver.getAnnotation());

    if(needsProxy) {
      try {
        proxyStrategy.createProxyFactory(Types.raw(dependentInjectable.getType()));
      }
      catch(Exception e) {
        throw new ScopeConflictException("Type [" + injectable.getType() + "] with scope [" + injectableScopeResolver.getAnnotation() + "] is dependent on [" + dependentInjectable.getType() + "] with normal scope [" + dependentScopeResolver.getAnnotation() + "]; this requires the use of a provider or proxy", e);
      }
    }
  }

  private void ensureNoCyclicDependencies(Collection<Injectable<?>> injectables) throws CyclicDependencyException {
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
          if(!binding.getTypeTraits().contains(TypeTrait.LAZY)) {
            for(Injectable<?> boundInjectable : qualifiedTypeStore.resolve(binding.getElementKey())) {
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
      throw new CyclicDependencyException(format(cycle));
    }
  }

  private static String format(List<? extends Injectable<?>> cycle) {
    StringBuilder b = new StringBuilder();

    b.append("     -----\n");
    b.append("    |     |\n");

    for(Injectable<?> i : cycle) {
      b.append("    |     V\n");
      b.append("    | " + i + "\n");
      b.append("    |     |\n");
    }

    b.append("     -----\n");

    return b.toString();
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
   * @return a {@link RegistrationViolation} if one was detected, otherwise {@code null}
   */
  private RegistrationViolation addSources(Collection<Injectable<?>> sources) {
    RegistrationViolation violation = null;

    for(Injectable<?> source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Map.Entry<Key, Node> entry : nodesByKeys.entrySet()) {
            Key key = entry.getKey();

            if(Types.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = entry.getValue();

              node.increaseSources(source);

              if(violation == null && node.isInvalid()) {
                violation = new RegistrationViolation(source, key);
              }
            }
          }
        }
      }
    }

    return violation;
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
   * @return a {@link RemoveViolation} if one was detected, otherwise {@code null}
   */
  private RemoveViolation removeSources(Collection<Injectable<?>> sources) {
    RemoveViolation violation = null;

    for(Injectable<?> source : sources) {
      Type type = source.getType();
      Set<Annotation> qualifiers = source.getQualifiers();

      for(Class<?> cls : Types.getSuperTypes(Types.raw(type))) {
        Map<Key, Node> nodesByKeys = nodes.get(cls);

        if(nodesByKeys != null) {
          for(Map.Entry<Key, Node> entry : nodesByKeys.entrySet()) {
            Key key = entry.getKey();

            if(Types.isAssignable(type, key.getType()) && qualifiers.containsAll(key.getQualifiers())) {
              Node node = entry.getValue();

              node.decreaseSources(source);

              if(violation == null && node.isInvalid()) {
                violation = new RemoveViolation(source);
              }
            }
          }
        }
      }
    }

    return violation;
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

  private class RegistrationViolation {
    final Injectable<?> source;
    final Key key;

    RegistrationViolation(Injectable<?> source, Key key) {
      this.source = source;
      this.key = key;
    }

    void doThrow() throws AmbiguousRequiredDependencyException {
      Set<Binding> bindings = findBindings(source.getType(), source.getQualifiers());  // expensive, but only for throwing exception
      String satisfiedBy = qualifiedTypeStore.resolve(key).stream().filter(i -> !i.equals(source)).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));

      throw new AmbiguousRequiredDependencyException("Registering [" + source + "] would make existing required bindings ambiguous: " + bindings + "; already satisfied by " + satisfiedBy);
    }
  }

  private class RemoveViolation {
    final Injectable<?> source;

    RemoveViolation(Injectable<?> source) {
      this.source = source;
    }

    void doThrow() throws UnsatisfiedRequiredDependencyException {
      Set<Binding> bindings = findBindings(source.getType(), source.getQualifiers());  // expensive, but only for throwing exception

      throw new UnsatisfiedRequiredDependencyException("Removing [" + source + "] would make existing required bindings unsatisfiable: " + bindings);
    }
  }

  /*
   * This call is very expensive, but worth it in the exceptional case to provide good error messages
   */
  private Set<Binding> findBindings(Type type, Set<Annotation> qualifiers) {
    return qualifiedTypeStore.toSet(s ->
      s.flatMap(i -> i.getBindings().stream())
        .filter(b -> Types.isAssignable(type, b.getType()) && qualifiers.containsAll(b.getQualifiers()))
        .collect(Collectors.toSet())
    );
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
