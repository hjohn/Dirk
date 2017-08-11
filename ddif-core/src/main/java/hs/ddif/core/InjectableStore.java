package hs.ddif.core;

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
 */
public class InjectableStore {

  /**
   * Map containing annotation descriptor mappings to sets of injectables which match one specific
   * type or qualifier class.<p>
   */
  private final Map<Class<?>, Map<AnnotationDescriptor, Set<Injectable>>> injectablesByDescriptorByType = new HashMap<>();

  private final StoreConsistencyPolicy policy;
  private final DiscoveryPolicy discoveryPolicy;

  public InjectableStore(StoreConsistencyPolicy policy, DiscoveryPolicy discoveryPolicy) {
    this.policy = policy == null ? new NoStoreConsistencyPolicy() : policy;
    this.discoveryPolicy = discoveryPolicy == null ? new NoDiscoveryPolicy() : discoveryPolicy;
  }

  public InjectableStore(StoreConsistencyPolicy policy) {
    this(policy, null);
  }

  public InjectableStore() {
    this(null, null);
  }

  Set<Injectable> resolve(Key key) {
    return resolve(key.getType(), (Object[])key.getQualifiersAsArray());
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
  public Set<Injectable> resolve(Type type, Object... criteria) {
    Class<?> cls = Binder.determineClassFromType(type);
    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(cls);

    if(injectablesByDescriptor == null) {

      /*
       * Attempt auto discovery
       */

      discoveryPolicy.discoverType(this, type);

      injectablesByDescriptor = injectablesByDescriptorByType.get(cls);  // Check again for matches after discovery

      if(injectablesByDescriptor == null) {
        return Collections.emptySet();
      }
    }

    Set<Injectable> matches = new HashSet<>(injectablesByDescriptor.get(null));  // Make a copy as otherwise retainAll below will modify the map

    filterByGenericType(type, matches);

    for(Object criterion : criteria) {
      if(matches.isEmpty()) {
        break;
      }

      if(criterion instanceof Matcher) {
        Matcher matcher = (Matcher)criterion;

        for(Iterator<Injectable> iterator = matches.iterator(); iterator.hasNext();) {
          Injectable injectable = iterator.next();

          if(!matcher.matches(injectable.getInjectableClass())) {
            iterator.remove();
          }
        }
      }
      else {
        Set<Injectable> qualifierMatches = null;

        if(criterion instanceof Class) {
          Map<AnnotationDescriptor, Set<Injectable>> map = injectablesByDescriptorByType.get(criterion);

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

    return matches;
  }

  public boolean contains(Class<?> concreteClass) {
    return injectablesByDescriptorByType.containsKey(concreteClass);
  }

  public void put(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }

    Class<?> concreteClass = injectable.getInjectableClass();

    if(concreteClass.getTypeParameters().length > 0) {
      throw new IllegalArgumentException(concreteClass + " has type parameters " + Arrays.toString(concreteClass.getTypeParameters()) + ": Injection candidates with type parameters are not supported.");
    }

    Set<AnnotationDescriptor> qualifiers = injectable.getQualifiers();

    policy.checkAddition(this, injectable, qualifiers);

    for(Class<?> type : getSuperClassesAndInterfaces(concreteClass)) {
      ensureRegistrationIsPossible(type, injectable);
    }

    /*
     * Beyond this point, modifications are made to the store, nothing should go wrong or the store's state could become inconsistent.
     */

    for(Class<?> type : getSuperClassesAndInterfaces(concreteClass)) {
      register(type, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        register(type, qualifier, injectable);
      }
    }
  }

  public void remove(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }

    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(Object.class);

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
    List<Class<?>> toScan = new ArrayList<>();
    Set<Class<?>> superClassesAndInterfaces = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Class<?> scanClassType = toScan.remove(toScan.size() - 1);
      superClassesAndInterfaces.add(scanClassType);

      Class<?> scanClass = Binder.determineClassFromType(scanClassType);

      for(Class<?> iface : scanClass.getInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getSuperclass());
      }
    }

    return superClassesAndInterfaces;
  }

  private void ensureRegistrationIsPossible(Class<?> type, Injectable injectable) {
    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      return;
    }

    Set<Injectable> injectables = injectablesByDescriptor.get(null);

    if(injectables == null || !injectables.contains(injectable)) {
      return;
    }

    throw new DuplicateBeanException(type, injectable);
  }

  private void register(Class<?> type, AnnotationDescriptor qualifier, Injectable injectable) {
    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      injectablesByDescriptor = new HashMap<>();
      injectablesByDescriptorByType.put(type, injectablesByDescriptor);
    }

    Set<Injectable> injectables = injectablesByDescriptor.get(qualifier);

    if(injectables == null) {
      injectables = new HashSet<>();
      injectablesByDescriptor.put(qualifier, injectables);
    }

    if(!injectables.add(injectable)) {
      throw new AssertionError("Map 'beanDefinitions' already contained: " + injectable + " for key: " + type + "->" + qualifier);
    }
  }

  private void removeInternal(Class<?> type, AnnotationDescriptor qualifier, Injectable injectable) {
    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {
      throw new AssertionError("Map 'beanDefinitions' must contain: " + injectable + " for key: " + type);
    }

    Set<Injectable> injectables = injectablesByDescriptor.get(qualifier);

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

  private static void filterByGenericType(Type type, Set<Injectable> matches) {
    if(type instanceof ParameterizedType) {
      for(Iterator<Injectable> iterator = matches.iterator(); iterator.hasNext();) {
        Injectable injectable = iterator.next();

        if(!TypeUtils.isAssignable(injectable.getInjectableClass(), type)) {
          iterator.remove();
        }
      }
    }
  }

  static class NoStoreConsistencyPolicy implements StoreConsistencyPolicy {
    @Override
    public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
    }

    @Override
    public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers) {
    }
  }

  static class NoDiscoveryPolicy implements DiscoveryPolicy {
    @Override
    public void discoverType(InjectableStore injectableStore, Type type) {
    }
  }
}
