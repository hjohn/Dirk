package hs.ddif.core.store;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Store which keeps track of {@link QualifiedType}s. A {@link StoreConsistencyPolicy} can
 * be used to track the additions and removals to the store and veto them if it would
 * violate the policy.
 *
 * <p>The store can be searched for {@link QualifiedType}s matching a {@link Key}. For
 * a {@link QualifiedType} to match it must be of the same type or a subtype of the
 * type in the key, and it must have all the qualifiers specified by the key.
 *
 * @param <T> the type of {@link QualifiedType}s this store holds
 */
public class QualifiedTypeStore<T extends QualifiedType> implements Resolver<T> {
  private final Comparator<Set<T>> comparatorConst = Comparator.comparingInt(Set::size);

  /**
   * Map containing qualifier annotation mappings to sets of {@link QualifiedType}s which match one specific
   * type or qualifier class.
   */
  private final Map<Class<?>, Map<Annotation, Set<T>>> qualifiedTypesByQualifierByType = new HashMap<>();

  private final StoreConsistencyPolicy<T> policy;

  /**
   * Constructs a new instance.
   *
   * @param policy a {@link StoreConsistencyPolicy} to enforce invariants, can be {@code null}
   */
  public QualifiedTypeStore(StoreConsistencyPolicy<T> policy) {
    this.policy = policy == null ? new NoStoreConsistencyPolicy() : policy;
  }

  /**
   * Constructs a new instance.
   */
  public QualifiedTypeStore() {
    this(null);
  }

  @Override
  public synchronized Set<T> resolve(Key key) {
    return resolve(key, List.of());
  }

  /**
   * Look up {@link QualifiedType}s by {@link Key} and a list of {@link Predicate}s.
   * For a {@link QualifiedType} to match it must be of the same type or a subtype of the
   * type in the key, and it must have all the qualifiers specified by the key.
   * The empty set is returned if there were no matches.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @param matchers a list of {@link Predicate}s, cannot be {@code null}
   * @return a set of {@link QualifiedType}s, never {@code null} and never contains {@code null}s but can be empty
   */
  public synchronized Set<T> resolve(Key key, Collection<Predicate<Type>> matchers) {
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
      Map<Annotation, Set<T>> qualifiedTypesByQualifier = qualifiedTypesByQualifierByType.get(Types.raw(type));

      if(qualifiedTypesByQualifier == null) {
        return Collections.emptySet();
      }

      if(key.getQualifiers().isEmpty()) {
        sets = Set.of(qualifiedTypesByQualifier.get(null));
      }
      else {
        sets = new PriorityQueue<>(comparatorConst);

        if(addQualifierBounds(key, sets, qualifiedTypesByQualifier)) {
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

    for(Predicate<Type> matcher : matchers) {
      filterByMatcher(matches, matcher);
    }

    return matches;
  }

  private boolean addUpperBound(Key key, Collection<Set<T>> sets, Class<?> upperBound) {
    Map<Annotation, Set<T>> qualifiedTypesByQualifier = qualifiedTypesByQualifierByType.get(upperBound);

    if(qualifiedTypesByQualifier == null) {
      return true;
    }

    if(!key.getQualifiers().isEmpty()) {
      return addQualifierBounds(key, sets, qualifiedTypesByQualifier);
    }

    sets.add(qualifiedTypesByQualifier.get(null));

    return false;
  }

  private boolean addQualifierBounds(Key key, Collection<Set<T>> sets, Map<Annotation, Set<T>> qualifiedTypesByQualifier) {
    for(Annotation annotation : key.getQualifiers()) {
      Set<T> set = qualifiedTypesByQualifier.get(annotation);

      if(set == null) {
        return true;
      }

      sets.add(set);
    }

    return false;
  }

  private void filterByMatcher(Set<T> matches, Predicate<Type> matcher) {
    for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
      T qualifiedType = iterator.next();

      if(!matcher.test(qualifiedType.getType())) {
        iterator.remove();
      }
    }
  }

  /**
   * Checks if there is a {@link QualifiedType} associated with the given {@link Key} and
   * matching the given {@link Predicate}s in the store.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @param matchers a list of {@link Predicate}s, cannot be {@code null}
   * @return {@code true} if there was a {@link QualifiedType} associated with the given {@link Key} and
   *   matching the given {@link Predicate}s, otherwise {@code false}
   */
  public synchronized boolean contains(Key key, List<Predicate<Type>> matchers) {
    return !resolve(key, matchers).isEmpty();
  }

  /**
   * Checks if there is a {@link QualifiedType} associated with the given {@link Key} in the store.
   *
   * @param key the {@link Key}, cannot be {@code null}
   * @return {@code true} if there was a {@link QualifiedType} associated with the given {@link Key},
   *   otherwise {@code false}
   */
  public synchronized boolean contains(Key key) {
    return contains(key, List.of());
  }

  /**
   * Adds a {@link QualifiedType} to the store.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   */
  public synchronized void put(T qualifiedType) {
    putAll(List.of(qualifiedType));
  }

  /**
   * Removes a {@link QualifiedType} from the store.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   */
  public synchronized void remove(T qualifiedType) {
    removeAll(List.of(qualifiedType));
  }

  /**
   * Adds multiple {@link QualifiedType}s to the store. If this method throws an exception then
   * the store will be unmodified.
   *
   * @param qualifiedTypes a collection of {@link QualifiedType}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public synchronized void putAll(Collection<T> qualifiedTypes) {
    for(T qualifiedType : qualifiedTypes) {
      ensureQualifiedTypeIsValid(qualifiedType);
    }

    // Duplication check must be done afterwards, as it can be duplicate with existing qualified types or within the group of added qualified types:
    List<T> addedQualifiedTypes = new ArrayList<>();

    try {
      for(T qualifiedType : qualifiedTypes) {
        ensureNotDuplicate(qualifiedType);
        putInternal(qualifiedType);
        addedQualifiedTypes.add(qualifiedType);
      }

      policy.addAll(this, qualifiedTypes);  // if this fails, policy will clean up after itself, no need to policy clean-up
    }
    catch(Exception e) {
      try {
        for(T qualifiedType : addedQualifiedTypes) {
          removeInternal(qualifiedType);
        }
      }
      catch(Exception e2) {
        AssertionError error = new AssertionError("Fatal error (store might be inconsistent) while adding: " + qualifiedTypes, e2);

        error.addSuppressed(e);

        throw error;
      }

      throw e;
    }
  }

  /**
   * Removes multiple {@link QualifiedType}s from the store. If this method throws an exception then
   * the store will be unmodified.
   *
   * @param qualifiedTypes a collection of {@link QualifiedType}s, cannot be {@code null} or contain {@code null}s but can be empty
   */
  public synchronized void removeAll(Collection<T> qualifiedTypes) {
    // First check the qualified types for fatal issues, exception does not need to be caught:
    for(T qualifiedType : qualifiedTypes) {
      ensureQualifiedTypeIsValid(qualifiedType);

      Map<Annotation, Set<T>> existingQualifiedTypes = qualifiedTypesByQualifierByType.get(TypeUtils.getRawType(qualifiedType.getType(), null));

      if(existingQualifiedTypes == null || !existingQualifiedTypes.get(null).contains(qualifiedType)) {
        throw new NoSuchQualifiedTypeException(qualifiedType);
      }
    }

    policy.removeAll(this, qualifiedTypes);  // if this fails, policy will clean up after itself, no need to do clean-up

    // Change the store, no exceptions should occur here:
    for(T qualifiedType : qualifiedTypes) {
      removeInternal(qualifiedType);
    }
  }

  /**
   * Returns a set with all {@link QualifiedType}s that are part of this store.
   *
   * @return a set with all {@link QualifiedType}s that are part of this store, never {@code null}
   *   or contains {@code null}s but can be empty
   */
  public synchronized Set<T> toSet() {
    return qualifiedTypesByQualifierByType.entrySet().stream()
      .filter(e -> e.getKey().isInterface() || e.getKey() == Object.class)  // although everything could be scanned, duplicates can be eliminated early here
      .map(Map.Entry::getValue)
      .map(Map::values)
      .flatMap(Collection::stream)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private void putInternal(T qualifiedType) {
    try {
      for(Class<?> type : Types.getSuperTypes(TypeUtils.getRawType(qualifiedType.getType(), null))) {
        if(type != Provider.class) {
          register(type, null, qualifiedType);

          for(Annotation qualifier : qualifiedType.getQualifiers()) {
            register(type, qualifier, qualifiedType);
          }
        }
      }
    }
    catch(Exception e) {
      throw new AssertionError("Fatal exception (store might be inconsistent) while adding: " + qualifiedType, e);
    }
  }

  private void removeInternal(T qualifiedType) {
    try {
      for(Class<?> type : Types.getSuperTypes(TypeUtils.getRawType(qualifiedType.getType(), null))) {
        if(type != Provider.class) {
          unregister(type, null, qualifiedType);

          for(Annotation qualifier : qualifiedType.getQualifiers()) {
            unregister(type, qualifier, qualifiedType);
          }
        }
      }
    }
    catch(Exception e) {
      throw new AssertionError("Fatal exception (store might be inconsistent) while removing: " + qualifiedType, e);
    }
  }

  private void ensureQualifiedTypeIsValid(T qualifiedType) {
    if(qualifiedType == null) {
      throw new IllegalArgumentException("qualifiedType cannot be null");
    }
  }

  private void ensureNotDuplicate(T qualifiedType) {
    Map<Annotation, Set<T>> qualifiedTypesByQualifier = qualifiedTypesByQualifierByType.get(Object.class);

    if(qualifiedTypesByQualifier != null && qualifiedTypesByQualifier.get(null).contains(qualifiedType)) {
      throw new DuplicateQualifiedTypeException(qualifiedType);
    }
  }

  private void register(Class<?> type, Annotation qualifier, T qualifiedType) {
    if(!qualifiedTypesByQualifierByType.computeIfAbsent(type, k -> new HashMap<>()).computeIfAbsent(qualifier, k -> new HashSet<>()).add(qualifiedType)) {
      throw new AssertionError("Store should not contain duplicates: " + qualifiedType);
    }
  }

  private void unregister(Class<?> type, Annotation qualifier, T qualifiedType) {
    Map<Annotation, Set<T>> qualifiedTypesByQualifier = qualifiedTypesByQualifierByType.get(type);

    if(qualifiedTypesByQualifier == null) {
      throw new AssertionError("Store must contain: " + qualifiedType + " for class: " + type);
    }

    Set<T> qualifiedTypes = qualifiedTypesByQualifier.get(qualifier);

    if(qualifiedTypes == null || !qualifiedTypes.remove(qualifiedType)) {
      throw new AssertionError("Store must contain: " + qualifiedType + " for class: " + type + " -> " + qualifier + " qualified types: " + qualifiedTypes);
    }

    if(qualifiedTypes.isEmpty()) {
      qualifiedTypesByQualifier.remove(qualifier);

      if(qualifiedTypesByQualifier.isEmpty()) {
        qualifiedTypesByQualifierByType.remove(type);
      }
    }
  }

  private void filterByGenericType(Type type, Set<T> matches) {
    if(type instanceof ParameterizedType) {
      for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
        T qualifiedType = iterator.next();

        if(!TypeUtils.isAssignable(qualifiedType.getType(), type)) {
          iterator.remove();
        }
      }
    }
  }

  class NoStoreConsistencyPolicy implements StoreConsistencyPolicy<T> {
    @Override
    public void addAll(Resolver<T> resolver, Collection<T> qualifiedTypes) {
      // No-op
    }

    @Override
    public void removeAll(Resolver<T> resolver, Collection<T> qualifiedTypes) {
      // No-op
    }
  }

  @Override
  public String toString() {
    return super.toString() + "[" + qualifiedTypesByQualifierByType + "]";
  }
}
