package hs.ddif.core.store;

import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Store which keeps track of injectable objects.  No effort is made to make sure that
 * the store only contains injectables that have resolvable bindings, although using a
 * {@link StoreConsistencyPolicy} it is possible to prevent such additions and removals
 * from taking place.
 *
 * @param <T> the type of {@link Injectable} this store holds
 */
public class InjectableStore<T extends Injectable> {

  /**
   * Map containing annotation descriptor mappings to sets of injectables which match one specific
   * type or qualifier class.<p>
   */
  private final Map<Class<?>, Map<AnnotationDescriptor, Set<T>>> injectablesByDescriptorByType = new HashMap<>();

  private final StoreConsistencyPolicy<T> policy;
  private final DiscoveryPolicy<T> discoveryPolicy;

  public InjectableStore(StoreConsistencyPolicy<T> policy, DiscoveryPolicy<T> discoveryPolicy) {
    this.policy = policy == null ? new NoStoreConsistencyPolicy() : policy;
    this.discoveryPolicy = discoveryPolicy == null ? new NoDiscoveryPolicy() : discoveryPolicy;
  }

  public InjectableStore(StoreConsistencyPolicy<T> policy) {
    this(policy, null);
  }

  public InjectableStore() {
    this(null, null);
  }

  /**
   * Looks up Injectables by type and by the given criteria.  The empty set is returned if
   * there were no matches.  Supported criteria are:
   * <ul>
   * <li>{@link Class} to match by implemented interface or by presence of an annotation, for
   *     example the interface <code>List.class</code> or the annotation
   *     <code>Singleton.class</code></li>
   * <li>{@link Annotation} or {@link AnnotationDescriptor} to match by an annotation,
   *     including matching all its values</li>
   * <li>{@link Matcher} to match by custom criteria provided by a {@link Matcher}
   *     implementation</li>
   * </ul>
   * @param type the type of the Injectables to look up
   * @param criteria the criteria the Injectables must match
   * @return a set of Injectables matching the given type and critera
   */
  public Set<T> resolve(Type type, Object... criteria) {
    Class<?> cls = TypeUtils.getRawType(type, null);
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(cls);

    if(injectablesByDescriptor == null && criteria.length == 0) {  // injectables with criteria cannot be auto discovered

      /*
       * Attempt auto discovery, only for beans without criteria.
       *
       * As discovering means attempting to instantiate a class of the required type, there can only
       * ever be one instance of such a class.  Distinguishing such a class with criteria therefore
       * is near useless, as such criteria can only be annotated directly on the class.  It would only
       * serve to restrict whether the class could be auto discovered or not -- all other combinations
       * of criteria would always fail as a class can only be annotated in one way (unlike Providers).
       */

      discoveryPolicy.discoverType(this, type);

      injectablesByDescriptor = injectablesByDescriptorByType.get(cls);  // Check again for matches after discovery
    }

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

      if(!matcher.matches(injectable.getInjectableClass())) {
        iterator.remove();
      }
    }
  }

  public boolean contains(Class<?> concreteClass) {
    return injectablesByDescriptorByType.containsKey(concreteClass);
  }

  public void put(T injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("injectable cannot be null");
    }

    Class<?> concreteClass = injectable.getInjectableClass();

    if(concreteClass.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(concreteClass + " has type parameters " + Arrays.toString(concreteClass.getTypeParameters()) + ": Injection candidates with type parameters are not supported.");
    }

    Set<AnnotationDescriptor> qualifiers = injectable.getQualifiers();

    policy.checkAddition(this, injectable, qualifiers);

    Set<Class<?>> superClassesAndInterfaces = getSuperClassesAndInterfaces(concreteClass);

    for(Class<?> type : superClassesAndInterfaces) {
      ensureRegistrationIsPossible(type, injectable);
    }

    /*
     * Beyond this point, modifications are made to the store, nothing should go wrong or the store's state could become inconsistent.
     */

    for(Class<?> type : superClassesAndInterfaces) {
      register(type, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        register(type, qualifier, injectable);
      }
    }
  }

  public void remove(T injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("injectable cannot be null");
    }

    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(Object.class);

    if(injectablesByDescriptor == null || !injectablesByDescriptor.get(null).contains(injectable)) {
      throw new NoSuchInjectableException(injectable);
    }

    Class<?> concreteClass = injectable.getInjectableClass();
    Set<AnnotationDescriptor> qualifiers = injectable.getQualifiers();  // TODO extractQualifiers might simply add ConcreteClass to the set?

    policy.checkRemoval(this, injectable, qualifiers);

    /*
     * Beyond this point, modifications are made to the store, nothing should go wrong or the store's state could become inconsistent.
     */

    for(Class<?> type : getSuperClassesAndInterfaces(concreteClass)) {
      removeInternal(type, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        removeInternal(type, qualifier, injectable);
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

  private void ensureRegistrationIsPossible(Class<?> type, Injectable injectable) {
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      return;
    }

    Set<T> injectables = injectablesByDescriptor.get(null);

    if(injectables == null || !injectables.contains(injectable)) {
      return;
    }

    throw new DuplicateBeanException(type, injectable);
  }

  private void register(Class<?> type, AnnotationDescriptor qualifier, T injectable) {
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      injectablesByDescriptor = new HashMap<>();
      injectablesByDescriptorByType.put(type, injectablesByDescriptor);
    }

    Set<T> injectables = injectablesByDescriptor.get(qualifier);

    if(injectables == null) {
      injectables = new HashSet<>();
      injectablesByDescriptor.put(qualifier, injectables);
    }

    if(!injectables.add(injectable)) {
      throw new AssertionError("Map 'beanDefinitions' already contained: " + injectable + " for key: " + type + "->" + qualifier);
    }
  }

  private void removeInternal(Class<?> type, AnnotationDescriptor qualifier, Injectable injectable) {
    Map<AnnotationDescriptor, Set<T>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      throw new AssertionError("Map 'beanDefinitions' must contain: " + injectable + " for key: " + type);
    }

    Set<T> injectables = injectablesByDescriptor.get(qualifier);

    if(injectables == null || !injectables.remove(injectable)) {
      throw new AssertionError("Map 'beanDefinitions' must contain: " + injectable + " for key: " + type + "->" + qualifier + " injectables = " + injectables);
    }

    if(injectables.isEmpty()) {
      injectablesByDescriptor.remove(qualifier);

      if(injectablesByDescriptor.isEmpty()) {
        injectablesByDescriptorByType.remove(type);
      }
    }
  }

  private void filterByGenericType(Type type, Set<T> matches) {
    if(type instanceof ParameterizedType) {
      for(Iterator<T> iterator = matches.iterator(); iterator.hasNext();) {
        Injectable injectable = iterator.next();

        if(!TypeUtils.isAssignable(injectable.getInjectableClass(), type)) {
          iterator.remove();
        }
      }
    }
  }

  class NoStoreConsistencyPolicy implements StoreConsistencyPolicy<T> {
    @Override
    public void checkAddition(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers) {
      // All additions are valid
    }

    @Override
    public void checkRemoval(InjectableStore<T> injectableStore, T injectable, Set<AnnotationDescriptor> qualifiers) {
      // All removals are valid
    }
  }

  class NoDiscoveryPolicy implements DiscoveryPolicy<T> {
    @Override
    public void discoverType(InjectableStore<T> injectableStore, Type type) {
      // Discover nothing
    }
  }
}
