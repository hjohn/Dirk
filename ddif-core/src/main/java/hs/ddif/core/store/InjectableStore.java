package hs.ddif.core.store;

import hs.ddif.core.api.Matcher;
import hs.ddif.core.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
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
import java.util.Queue;
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
    return resolve(key, List.of(), List.of());
  }

  public synchronized Set<T> resolve(Key key, Criteria criteria) {
    return resolve(key, criteria.getInterfaces(), criteria.getMatchers());
  }

  public synchronized Set<T> resolve(Key key, Collection<Class<?>> interfaces, Collection<Matcher> matchers) {
    Class<?> cls = TypeUtils.getRawType(key.getType(), null);
    Map<Annotation, Set<T>> injectablesByAnnotation = injectablesByAnnotationByType.get(cls);

    if(injectablesByAnnotation == null) {
      return Collections.emptySet();
    }

    /*
     * The sets can be very large, and copying a large set is a waste. Therefore
     * gather all sets sorted by size. Copy the smallest set and perform retainAll
     * with the remaining sets.
     */

    Queue<Set<T>> sets = new PriorityQueue<>(comparatorConst);

    for(Annotation annotation : key.getQualifiers()) {
      Set<T> set = injectablesByAnnotation.get(annotation);

      if(set == null) {
        return Collections.emptySet();
      }

      sets.add(set);
    }

    for(Class<?> iface : interfaces) {
      Map<Annotation, Set<T>> map = injectablesByAnnotationByType.get(iface);

      if(map == null) {
        return Collections.emptySet();
      }

      Set<T> set = map.get(null);

      if(set == null) {
        return Collections.emptySet();
      }

      sets.add(set);
    }

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
     * If at this point matches is still null, then there were no filters by qualifier or interface.
     * A copy must be made then of the set containing all injectables for a class.
     */

    matches = matches == null ? new HashSet<>(injectablesByAnnotation.get(null)) : matches;

    /*
     * If necessary, further strip down the matches based on an exact generic type match:
     */

    filterByGenericType(key.getType(), matches);

    /*
     * Finally apply custom supplied matchers:
     */

    for(Matcher matcher : matchers) {
      filterByMatcher(matches, matcher);
    }

    return matches;
  }

  private static <T extends Injectable> void filterByMatcher(Set<T> matches, Matcher matcher) {
    for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
      Injectable injectable = iterator.next();

      if(!matcher.matches(TypeUtils.getRawType(injectable.getType(), null))) {
        iterator.remove();
      }
    }
  }

  public synchronized boolean contains(Key key, Criteria criteria) {
    return !resolve(key, criteria).isEmpty();
  }

  public synchronized boolean contains(Key key) {
    return contains(key, Criteria.EMPTY);
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
