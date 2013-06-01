package hs.ddif;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
   * Map containing sets of injectables which match one specific type or qualifier.  An
   * injectable is added multiple times to this Map, once for each qualifier, class,
   * superclass and interface it matches.<p>
   *
   * The key is either an Annotation (annotated with {@link Qualifier}) or a {@link Class}.
   */
  private final Map<Object, Set<Injectable>> beanDefinitions = new HashMap<>();

  /**
   * Map containing bindings for each class.  Bindings can resolved to a value that can
   * be injected into a new object.  Bindings can be optional or required.  Required
   * Bindings play an important role when ensuring that all dependencies an injectable
   * has can be resolved at runtime.<p>
   *
   * The key is always a concrete class.
   */
  private final Map<Class<?>, Map<AccessibleObject, Binding>> classBindings = new HashMap<>();

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
    return Collections.unmodifiableMap(classBindings.get(cls));
  }

  public Set<Class<?>> getInjectables() {
    return Collections.unmodifiableSet(classBindings.keySet());
  }

  public Set<Injectable> resolve(Key key) {
    Set<Injectable> matches = beanDefinitions.get(key.getType());

    if(matches == null) {

      /*
       * Attempt auto discovery
       */

      discoveryPolicy.discoverType(this, key.getType());

      matches = beanDefinitions.get(key.getType());  // Check again for matches after discovery

      if(matches == null) {
        return Collections.emptySet();
      }
    }

    matches = new HashSet<>(matches);  // Make a copy as otherwise retainAll below will modify the beanDefinitions map

    for(Annotation qualifier : key.getQualifiers()) {
      Set<Injectable> qualifierMatches = beanDefinitions.get(qualifier);

      if(qualifierMatches == null) {
        return Collections.emptySet();
      }

      matches.retainAll(qualifierMatches);
    }

    return matches;
  }

  public boolean contains(Class<?> concreteClass) {
    return classBindings.containsKey(concreteClass);
  }

  public Map<AccessibleObject, Binding> put(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }

    Class<?> concreteClass = injectable.getInjectableClass();

    if(classBindings.containsKey(concreteClass)) {
      throw new DuplicateBeanException(concreteClass);
    }

    Set<Annotation> qualifiers = extractQualifiers(concreteClass);
    Map<AccessibleObject, Binding> bindings = binder.resolve(concreteClass);

    discoveryPolicy.discoverDependencies(this, injectable, bindings);
    policy.checkAddition(this, injectable, qualifiers, bindings);

    classBindings.put(concreteClass, bindings);

    for(Annotation annotation : qualifiers) {
      register(annotation, injectable);
    }
    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      register(cls, injectable);
    }

    return bindings;
  }

  public Map<AccessibleObject, Binding> remove(Injectable injectable) {
    if(injectable == null) {
      throw new IllegalArgumentException("parameter 'injectable' cannot be null");
    }
    if(!beanDefinitions.get(Object.class).contains(injectable)) {
      throw new NoSuchInjectableException(injectable);
    }

    Class<?> concreteClass = injectable.getInjectableClass();
    Set<Annotation> qualifiers = extractQualifiers(concreteClass);  // TODO extractQualifiers might simply add ConcreteClass to the set?

    policy.checkRemoval(this, injectable, qualifiers, classBindings.get(injectable.getInjectableClass()));

    for(Annotation annotation : qualifiers) {
      removeInternal(annotation, injectable);
    }
    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      removeInternal(cls, injectable);
    }

    return classBindings.remove(concreteClass);
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

  private void register(Object typeOrQualifier, Injectable injectable) {
    Set<Injectable> values = beanDefinitions.get(typeOrQualifier);

    if(values == null) {
      values = new HashSet<>();
      beanDefinitions.put(typeOrQualifier, values);
    }

    if(!values.add(injectable)) {
      throw new AssertionError("Map 'beanDefinitions' already contained: " + injectable + " for key: " + typeOrQualifier);
    }
  }

  private void removeInternal(Object typeOrQualifier, Injectable injectable) {
    Set<Injectable> values = beanDefinitions.get(typeOrQualifier);

    if(values == null || !values.remove(injectable)) {
      throw new AssertionError("Map 'beanDefinitions' must contain: " + injectable + " for key: " + typeOrQualifier + " concreteClasses = " + values);
    }

    if(values.isEmpty()) {
      beanDefinitions.remove(typeOrQualifier);
    }
  }

  private static Set<Annotation> extractQualifiers(Class<?> cls) {
    return extractQualifiers(cls.getAnnotations());
  }

  private static Set<Annotation> extractQualifiers(Annotation[] annotations) {
    Set<Annotation> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(annotation);
      }
    }

    return qualifiers;
  }

  static class NoStoreConsistencyPolicy implements StoreConsistencyPolicy {

    @Override
    public void checkAddition(InjectableStore injectableStore, Injectable injectable, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
    }

    @Override
    public void checkRemoval(InjectableStore injectableStore, Injectable injectable, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
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
