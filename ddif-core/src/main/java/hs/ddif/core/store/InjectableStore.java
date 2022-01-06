package hs.ddif.core.store;

import hs.ddif.core.api.Matcher;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Store which keeps track of injectable objects.  No effort is made to make sure that
 * the store only contains injectables that have resolvable bindings, although using a
 * {@link StoreConsistencyPolicy} it is possible to prevent such additions and removals
 * from taking place.
 *
 * @param <T> the type of {@link Injectable} this store holds
 */
public class InjectableStore<T extends Injectable> implements Resolver<T> {
  private final Comparator<Set<T>> comparatorConst = Comparator.comparingInt(Set::size);

  /**
   * Map containing annotation mappings to sets of injectables which match one specific
   * type or qualifier class.
   */
  private final Map<Class<?>, Map<Annotation, Set<T>>> injectablesByAnnotationByType = new HashMap<>();

  private final StoreConsistencyPolicy<T> policy;

  /**
   * Constructs a new instance.
   *
   * @param policy a {@link StoreConsistencyPolicy} to enforce invariants, can be null
   */
  public InjectableStore(StoreConsistencyPolicy<T> policy) {
    this.policy = policy == null ? new NoStoreConsistencyPolicy() : policy;
  }

  /**
   * Constructs a new instance.
   */
  public InjectableStore() {
    this(null);
  }

  @Override
  public synchronized Set<T> resolve(Key key) {
    return resolve(key, List.of());
  }

  /**
   * Look up injectables by {@link Key} and a list of {@link Matcher}s. The empty set is returned if
   * there were no matches.
   *
   * @param key the {@link Key}, cannot be null
   * @param matchers a list of {@link Matcher}s, cannot be null
   * @return a set of injectables, never null but can be empty
   */
  public synchronized Set<T> resolve(Key key, Collection<Matcher> matchers) {
    Type type = key.getType();
    Collection<Set<T>> sets;
    Set<Type> upperBounds;

    if(type instanceof WildcardType) {
      sets = new PriorityQueue<>(comparatorConst);
      upperBounds = Set.of(TypeUtils.getImplicitUpperBounds((WildcardType)type));

      for(Type upperBound : upperBounds) {
        if(addUpperBound(key, sets, Types.raw(upperBound))) {
          return Collections.emptySet();
        }
      }
    }
    else {
      Map<Annotation, Set<T>> injectablesByAnnotation = injectablesByAnnotationByType.get(Types.raw(type));

      if(injectablesByAnnotation == null) {
        return Collections.emptySet();
      }

      if(key.getQualifiers().isEmpty()) {
        sets = Set.of(injectablesByAnnotation.get(null));
      }
      else {
        sets = new PriorityQueue<>(comparatorConst);

        if(addQualifierBounds(key, sets, injectablesByAnnotation)) {
          return Collections.emptySet();
        }
      }

      upperBounds = Set.of(type);
    }

    /*
     * The sets can be very large, and copying a large set is a waste. Therefore
     * gather all sets sorted by size. Copy the smallest set and perform retainAll
     * with the remaining sets.
     */

    Set<T> matches = null;

    for(Set<T> set : sets) {
      if(matches == null) {
        matches = new HashSet<>(set);  // copies smallest set
      }
      else {
        matches.retainAll(set);  // retains on smallest set with next smallest set
      }

      if(matches.isEmpty()) {
        return Collections.emptySet();
      }
    }

    /*
     * If necessary, further strip down the matches based on an exact generic type match:
     */

    for(Type upperBound : upperBounds) {
      filterByGenericType(upperBound, matches);
    }

    /*
     * Finally apply custom supplied matchers:
     */

    for(Matcher matcher : matchers) {
      filterByMatcher(matches, matcher);
    }

    return matches;
  }

  private boolean addUpperBound(Key key, Collection<Set<T>> sets, Class<?> upperBound) {
    Map<Annotation, Set<T>> injectablesByAnnotation = injectablesByAnnotationByType.get(upperBound);

    if(injectablesByAnnotation == null) {
      return true;
    }

    if(!key.getQualifiers().isEmpty()) {
      return addQualifierBounds(key, sets, injectablesByAnnotation);
    }

    sets.add(injectablesByAnnotation.get(null));

    return false;
  }

  private boolean addQualifierBounds(Key key, Collection<Set<T>> sets, Map<Annotation, Set<T>> injectablesByAnnotation) {
    for(Annotation annotation : key.getQualifiers()) {
      Set<T> set = injectablesByAnnotation.get(annotation);

      if(set == null) {
        return true;
      }

      sets.add(set);
    }

    return false;
  }

  private static <T extends Injectable> void filterByMatcher(Set<T> matches, Matcher matcher) {
    for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
      Injectable injectable = iterator.next();

      if(!matcher.matches(TypeUtils.getRawType(injectable.getType(), null))) {
        iterator.remove();
      }
    }
  }

  /**
   * Checks if there is an injectable in the store matching the given {@link Key}
   * and a list of {@link Matcher}s.
   *
   * @param key the {@link Key}, cannot be null
   * @param matchers a list of {@link Matcher}s, cannot be null
   * @return {@code true} if there was an injectable matching the key and criteria, otherwise {@code false}
   */
  public synchronized boolean contains(Key key, List<Matcher> matchers) {
    return !resolve(key, matchers).isEmpty();
  }

  /**
   * Checks if there is an injectable in the store matching the given {@link Key}.
   *
   * @param key the {@link Key}, cannot be null
   * @return {@code true} if there was an injectable matching the key, otherwise {@code false}
   */
  public synchronized boolean contains(Key key) {
    return contains(key, List.of());
  }

  /**
   * Adds an {@link Injectable} to the store.
   *
   * @param injectable an {@link Injectable}, cannot be null
   * @throws NullPointerException when given a null {@link Injectable}
   */
  public synchronized void put(T injectable) {
    putAll(List.of(injectable));
  }

  /**
   * Adds an {@link Injectable} to the store.
   *
   * @param injectable an {@link Injectable}, cannot be null
   * @throws NullPointerException when given a null {@link Injectable}
   */
  public synchronized void remove(T injectable) {
    removeAll(List.of(injectable));
  }

  /**
   * Adds {@link Injectable}s to the store.
   *
   * @param injectables a collection of {@link Injectable}s, cannot be null or contain nulls but can be empty
   * @throws NullPointerException when injectables was null or contained nulls
   */
  public synchronized void putAll(Collection<T> injectables) {
    for(T injectable : injectables) {
      ensureInjectableIsValid(injectable);
    }

    // Duplication check must be done afterwards, as it can be duplicate with existing injectables or within the group of added injectables:
    List<T> addedInjectables = new ArrayList<>();

    try {
      for(T injectable : injectables) {
        ensureNotDuplicate(injectable);
        putInternal(injectable);
        addedInjectables.add(injectable);
      }

      policy.addAll(this, injectables);  // if this fails, policy will clean up after itself, no need to policy clean-up
    }
    catch(Exception e) {
      try {
        for(T injectable : addedInjectables) {
          removeInternal(injectable);
        }
      }
      catch(Exception e2) {
        IllegalStateException illegalStateException = new IllegalStateException("Fatal exception (store might be inconsistent) while adding: " + injectables, e2);

        illegalStateException.addSuppressed(e);

        throw illegalStateException;
      }

      throw e;
    }
  }

  /**
   * Removes {@link Injectable}s from the store.
   *
   * @param injectables a collection of {@link Injectable}s, cannot be null or contain nulls but can be empty
   * @throws NullPointerException when injectables was null or contained nulls
   */
  public synchronized void removeAll(Collection<T> injectables) {
    // First check injectables for fatal issues, exception does not need to be caught:
    for(T injectable : injectables) {
      ensureInjectableIsValid(injectable);

      Map<Annotation, Set<T>> specificInjectables = injectablesByAnnotationByType.get(TypeUtils.getRawType(injectable.getType(), null));

      if(specificInjectables == null || !specificInjectables.get(null).contains(injectable)) {
        throw new NoSuchInjectableException(injectable);
      }
    }

    policy.removeAll(this, injectables);  // if this fails, policy will clean up after itself, no need to do clean-up

    // Change the store, no exceptions should occur here:
    for(T injectable : injectables) {
      removeInternal(injectable);
    }
  }

  /**
   * Returns a set with all injectables that are part of this store.
   *
   * @return a set with all injectables that are part of this store, never null
   *   but can be empty
   */
  public synchronized Set<T> toSet() {
    return injectablesByAnnotationByType.entrySet().stream()
      .filter(e -> e.getKey().isInterface() || e.getKey() == Object.class)  // although everything could be scanned, duplicates can be elimated early here
      .map(Map.Entry::getValue)
      .map(Map::values)
      .flatMap(Collection::stream)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private void putInternal(T injectable) {
    try {
      for(Class<?> type : Types.getSuperTypes(TypeUtils.getRawType(injectable.getType(), null))) {
        if(type != Provider.class) {
          register(type, null, injectable);

          for(Annotation qualifier : injectable.getQualifiers()) {
            register(type, qualifier, injectable);
          }
        }
      }
    }
    catch(Exception e) {
      throw new IllegalStateException("Fatal exception (store might be inconsistent) while adding: " + injectable, e);
    }
  }

  private void removeInternal(T injectable) {
    try {
      for(Class<?> type : Types.getSuperTypes(TypeUtils.getRawType(injectable.getType(), null))) {
        if(type != Provider.class) {
          unregister(type, null, injectable);

          for(Annotation qualifier : injectable.getQualifiers()) {
            unregister(type, qualifier, injectable);
          }
        }
      }
    }
    catch(Exception e) {
      throw new IllegalStateException("Fatal exception (store might be inconsistent) while removing: " + injectable, e);
    }
  }

  private void ensureInjectableIsValid(T injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("injectable cannot be null");
    }
  }

  private void ensureNotDuplicate(T injectable) {
    Map<Annotation, Set<T>> injectablesByAnnotation = injectablesByAnnotationByType.get(Object.class);

    if(injectablesByAnnotation != null && injectablesByAnnotation.get(null).contains(injectable)) {
      throw new DuplicateInjectableException(TypeUtils.getRawType(injectable.getType(), null), injectable);
    }
  }

  private void register(Class<?> type, Annotation qualifier, T injectable) {
    if(!injectablesByAnnotationByType.computeIfAbsent(type, k -> new HashMap<>()).computeIfAbsent(qualifier, k -> new HashSet<>()).add(injectable)) {
      throw new AssertionError("Store should not contain duplicates: " + injectable);
    }
  }

  private void unregister(Class<?> type, Annotation qualifier, Injectable injectable) {
    Map<Annotation, Set<T>> injectablesByAnnotation = injectablesByAnnotationByType.get(type);

    if(injectablesByAnnotation == null) {
      throw new AssertionError("Store must contain: " + injectable + " for key: " + type);
    }

    Set<T> injectables = injectablesByAnnotation.get(qualifier);

    if(injectables == null || !injectables.remove(injectable)) {
      throw new AssertionError("Store must contain: " + injectable + " for key: " + type + " -> " + qualifier + " injectables: " + injectables);
    }

    if(injectables.isEmpty()) {
      injectablesByAnnotation.remove(qualifier);

      if(injectablesByAnnotation.isEmpty()) {
        injectablesByAnnotationByType.remove(type);
      }
    }
  }

  private static void filterByGenericType(Type type, Set<? extends Injectable> matches) {
    if(type instanceof ParameterizedType) {
      for(Iterator<? extends Injectable> iterator = matches.iterator(); iterator.hasNext();) {
        Injectable injectable = iterator.next();

        if(!TypeUtils.isAssignable(injectable.getType(), type)) {
          iterator.remove();
        }
      }
    }
  }

  class NoStoreConsistencyPolicy implements StoreConsistencyPolicy<T> {
    @Override
    public void addAll(Resolver<T> resolver, Collection<T> injectables) {
      // No-op
    }

    @Override
    public void removeAll(Resolver<T> resolver, Collection<T> injectables) {
      // No-op
    }
  }

  @Override
  public String toString() {
    return super.toString() + "[" + injectablesByAnnotationByType + "]";
  }
}
