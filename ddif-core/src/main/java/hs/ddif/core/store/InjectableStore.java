package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

  /**
   * Map containing annotation descriptor mappings to sets of injectables which match one specific
   * type or qualifier class.<p>
   */
  private final Map<Class<?>, Map<AnnotationDescriptor, Set<T>>> injectablesByDescriptorByType = new HashMap<>();

  private final StoreConsistencyPolicy<T> policy;

  public InjectableStore(StoreConsistencyPolicy<T> policy) {
    this.policy = policy == null ? new NoStoreConsistencyPolicy() : policy;
  }

  public InjectableStore() {
    this(null);
  }

  @Override
  public synchronized Set<T> resolve(Type type, Object... criteria) {
    Class<?> cls = TypeUtils.getRawType(type, null);
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(cls);

    if(injectablesByDescriptor == null) {
      return Collections.emptySet();
    }

    Set<T> matches = new HashSet<>(injectablesByDescriptor.get(null));  // Make a copy as otherwise retainAll below will modify the map

    filterByGenericType(type, matches);
    filterByCriteria(injectablesByDescriptor, matches, criteria);

    return matches;
  }

  private void filterByCriteria(Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor, Set<T> matches, Object... criteria) {
    for(Object criterion : criteria) {
      if(matches.isEmpty()) {
        break;
      }

      if(criterion instanceof Matcher) {
        filterByMatcher(matches, (Matcher)criterion);
        continue;  // Skip matches standard filtering
      }

      Set<T> qualifierMatches = null;

      if(criterion instanceof Class && ((Class<?>)criterion).isAnnotation()) {  // If an annotation is passed in as a class, convert it to its descriptor
        @SuppressWarnings("unchecked")
        Class<? extends Annotation> castedCriterion = (Class<? extends Annotation>)criterion;

        criterion = AnnotationDescriptor.describe(castedCriterion);
      }

      if(criterion instanceof Class) {
        Map<AnnotationDescriptor, Set<T>> map = injectablesByDescriptorByType.get(criterion);

        if(map != null) {
          qualifierMatches = map.get(null);
        }
      }
      else if(criterion instanceof Annotation) {
        qualifierMatches = injectablesByDescriptor.get(new AnnotationDescriptor((Annotation)criterion));
      }
      else if(criterion instanceof AnnotationDescriptor) {
        qualifierMatches = injectablesByDescriptor.get(criterion);
      }
      else {
        throw new IllegalArgumentException("Unsupported criterion type, must be Class, Annotation or Matcher: " + criterion);
      }

      if(qualifierMatches != null) {
        matches.retainAll(qualifierMatches);
      }
      else {
        matches.clear();
      }
    }
  }

  private void filterByMatcher(Set<T> matches, Matcher matcher) {
    for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
      Injectable injectable = iterator.next();

      if(!matcher.matches(TypeUtils.getRawType(injectable.getType(), null))) {
        iterator.remove();
      }
    }
  }

  public synchronized boolean contains(Class<?> concreteClass) {
    return injectablesByDescriptorByType.containsKey(concreteClass);
  }

  public synchronized boolean contains(Type type, Object... criteria) {
    Class<?> cls = TypeUtils.getRawType(type, null);
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(cls);

    if(injectablesByDescriptor == null) {
      return false;
    }

    Set<T> matches = new HashSet<>(injectablesByDescriptor.get(null));  // Make a copy as otherwise retainAll below will modify the map

    filterByGenericType(type, matches);
    filterByCriteria(injectablesByDescriptor, matches, criteria);

    return !matches.isEmpty();
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

      Map<AnnotationDescriptor, Set<T>> specificInjectables = injectablesByDescriptorByType.get(TypeUtils.getRawType(injectable.getType(), null));

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
    return injectablesByDescriptorByType.entrySet().stream()
      .filter(e -> e.getKey().isInterface() || e.getKey() == Object.class)  // although everything could be scanned, duplicates can be elimated early here
      .map(Map.Entry::getValue)
      .map(Map::values)
      .flatMap(Collection::stream)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private void putInternal(T injectable) {
    try {
      for(Class<?> superType : getSuperClassesAndInterfaces(TypeUtils.getRawType(injectable.getType(), null))) {
        register(superType, null, injectable);

        for(AnnotationDescriptor qualifier : injectable.getQualifiers()) {
          register(superType, qualifier, injectable);
        }
      }
    }
    catch(Exception e) {
      throw new IllegalStateException("Fatal exception (store might be inconsistent) while adding: " + injectable, e);
    }
  }

  private void removeInternal(T injectable) {
    try {
      for(Class<?> type : getSuperClassesAndInterfaces(TypeUtils.getRawType(injectable.getType(), null))) {
        unregister(type, null, injectable);

        for(AnnotationDescriptor qualifier : injectable.getQualifiers()) {
          unregister(type, qualifier, injectable);
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
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(Object.class);

    if(injectablesByDescriptor != null && injectablesByDescriptor.get(null).contains(injectable)) {
      throw new DuplicateBeanException(TypeUtils.getRawType(injectable.getType(), null), injectable);
    }
  }

  private void register(Class<?> type, AnnotationDescriptor qualifier, T injectable) {
    if(!injectablesByDescriptorByType.computeIfAbsent(type, k -> new HashMap<>()).computeIfAbsent(qualifier, k -> new HashSet<>()).add(injectable)) {
      throw new AssertionError("Store should not contain duplicates: " + injectable);
    }
  }

  private void unregister(Class<?> type, AnnotationDescriptor qualifier, Injectable injectable) {
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      throw new AssertionError("Store must contain: " + injectable + " for key: " + type);
    }

    Set<T> injectables = injectablesByDescriptor.get(qualifier);

    if(injectables == null || !injectables.remove(injectable)) {
      throw new AssertionError("Store must contain: " + injectable + " for key: " + type + " -> " + qualifier + " injectables: " + injectables);
    }

    if(injectables.isEmpty()) {
      injectablesByDescriptor.remove(qualifier);

      if(injectablesByDescriptor.isEmpty()) {
        injectablesByDescriptorByType.remove(type);
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

  private static Set<Class<?>> getSuperClassesAndInterfaces(Class<?> cls) {
    List<Type> toScan = new ArrayList<>();
    Set<Class<?>> superClassesAndInterfaces = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Type scanClassType = toScan.remove(toScan.size() - 1);

      Class<?> scanClass = TypeUtils.getRawType(scanClassType, null);

      superClassesAndInterfaces.add(scanClass);

      for(Type iface : scanClass.getGenericInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getGenericSuperclass());
      }
    }

    return superClassesAndInterfaces;
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
    return super.toString() + "[" + injectablesByDescriptorByType + "]";
  }
}
