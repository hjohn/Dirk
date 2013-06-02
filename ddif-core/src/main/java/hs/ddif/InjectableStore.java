package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Store which keeps track of injectable objects and of their bindings.  No effort
 * is made to make sure that the store only contains injectables that have
 * resolvable bindings, although using a {@link StoreConsistencyPolicy} it is
 * possible to prevent such additions and removals from taking place.
 *
 * @param <T> type of objects the store holds
 */
public class InjectableStore {

  /**
   * Map containing annotation descriptor mappings to sets of injectables which match one specific
   * type or qualifier class.<p>
   */
  private final Map<Class<?>, Map<AnnotationDescriptor, Set<Injectable>>> injectablesByDescriptorByType = new HashMap<>();

  /**
   * Map containing bindings for each class.  Bindings can resolved to a value that can
   * be injected into a new object.  Bindings can be optional or required.  Required
   * Bindings play an important role when ensuring that all dependencies an injectable
   * has can be resolved at runtime.<p>
   *
   * The key is always a concrete class.
   */
  private final Map<Class<?>, Map<AccessibleObject, Binding>> bindingsByClass = new HashMap<>();

  private final Binder binder = new Binder();
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

  public Map<AccessibleObject, Binding> getBindings(Class<?> cls) {
    return Collections.unmodifiableMap(bindingsByClass.get(cls));
  }

  public Set<Class<?>> getInjectables() {
    return Collections.unmodifiableSet(bindingsByClass.keySet());
  }

  Set<Injectable> resolve(Key key) {
    return resolve(key.getType(), (Object[])key.getQualifiers());
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
  public Set<Injectable> resolve(Class<?> type, Object... criteria) {
    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(type);

    if(injectablesByDescriptor == null) {

      /*
       * Attempt auto discovery
       */

      discoveryPolicy.discoverType(this, type);

      injectablesByDescriptor = injectablesByDescriptorByType.get(type);  // Check again for matches after discovery

      if(injectablesByDescriptor == null) {
        return Collections.emptySet();
      }
    }

    Set<Injectable> matches = new HashSet<>(injectablesByDescriptor.get(null));  // Make a copy as otherwise retainAll below will modify the map

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
    return bindingsByClass.containsKey(concreteClass);
  }

  public Map<AccessibleObject, Binding> put(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }

    Class<?> concreteClass = injectable.getInjectableClass();

    if(bindingsByClass.containsKey(concreteClass)) {
      throw new DuplicateBeanException(concreteClass);
    }

    Set<AnnotationDescriptor> qualifiers = extractQualifiers(concreteClass);
    Map<AccessibleObject, Binding> bindings = binder.resolve(concreteClass);

    discoveryPolicy.discoverDependencies(this, injectable, bindings);
    policy.checkAddition(this, injectable, qualifiers, bindings);

    bindingsByClass.put(concreteClass, bindings);

    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      register(cls, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        register(cls, qualifier, injectable);
      }
    }

    for(AnnotationDescriptor outerQualifier : qualifiers) {
      Class<? extends Annotation> annotationType = outerQualifier.annotationType();

      register(annotationType, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        register(annotationType, qualifier, injectable);
      }
    }

    return bindings;
  }

  public Map<AccessibleObject, Binding> remove(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }

    Map<AnnotationDescriptor, Set<Injectable>> injectablesByDescriptor = injectablesByDescriptorByType.get(Object.class);

    if(injectablesByDescriptor == null || !injectablesByDescriptor.get(null).contains(injectable)) {
      throw new NoSuchInjectableException(injectable);
    }

    Class<?> concreteClass = injectable.getInjectableClass();
    Set<AnnotationDescriptor> qualifiers = extractQualifiers(concreteClass);  // TODO extractQualifiers might simply add ConcreteClass to the set?

    policy.checkRemoval(this, injectable, qualifiers, bindingsByClass.get(injectable.getInjectableClass()));

    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      removeInternal(cls, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        removeInternal(cls, qualifier, injectable);
      }
    }

    for(AnnotationDescriptor outerQualifier : qualifiers) {
      Class<? extends Annotation> annotationType = outerQualifier.annotationType();

      removeInternal(annotationType, null, injectable);

      for(AnnotationDescriptor qualifier : qualifiers) {
        removeInternal(annotationType, qualifier, injectable);
      }
    }

    return bindingsByClass.remove(concreteClass);
  }

  private static Set<Class<?>> getSuperClassesAndInterfaces(Class<?> cls) {
    List<Class<?>> toScan = new ArrayList<>();
    Set<Class<?>> superClassesAndInterfaces = new HashSet<>();

    toScan.add(cls);

    while(!toScan.isEmpty()) {
      Class<?> scanClass = toScan.remove(toScan.size() - 1);
      superClassesAndInterfaces.add(scanClass);

      for(Class<?> iface : scanClass.getInterfaces()) {
        toScan.add(iface);
      }

      if(scanClass.getSuperclass() != null) {
        toScan.add(scanClass.getSuperclass());
      }
    }

    return superClassesAndInterfaces;
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

  private static Set<AnnotationDescriptor> extractQualifiers(Class<?> cls) {
    return extractQualifiers(cls.getAnnotations());
  }

  private static Set<AnnotationDescriptor> extractQualifiers(Annotation[] annotations) {
    Set<AnnotationDescriptor> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(new AnnotationDescriptor(annotation));
      }
    }

    return qualifiers;
  }

  static class NoStoreConsistencyPolicy implements StoreConsistencyPolicy {

    @Override
    public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers, Map<AccessibleObject, Binding> bindings) {
    }

    @Override
    public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<AnnotationDescriptor> qualifiers, Map<AccessibleObject, Binding> bindings) {
    }
  }

  static class NoDiscoveryPolicy implements DiscoveryPolicy {

    @Override
    public void discoverType(InjectableStore injectableStore, Class<?> type) {
    }

    @Override
    public void discoverDependencies(InjectableStore injectableStore, Injectable injectable, Map<AccessibleObject, Binding> bindings) {
    }
  }
}