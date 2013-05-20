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
 */
public class InjectableStore {

  /**
   * Map containing sets of injectables which match one specific type or qualifier.  An
   * injectable is added multiple times to this Map, once for each qualifier, class,
   * superclass and interface it matches.<p>
   *
   * The key is either an Annotation (annotated with {@link Qualifier}) or a {@link Class}.
   */
  private final Map<Object, Set<Class<?>>> beanDefinitions = new HashMap<>();

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

  public InjectableStore(StoreConsistencyPolicy policy) {
    this.policy = policy;
  }

  public InjectableStore() {
    this(new StoreConsistencyPolicy() {
      @Override
      public void checkAddition(Class<?> concreteClass, Set<Annotation> qualifiers, Map<AccessibleObject, Binding> bindings) {
      }

      @Override
      public void checkRemoval(Class<?> concreteClass, Set<Annotation> qualifiers) {
      }
    });
  }

  public Map<AccessibleObject, Binding> getInjections(Class<?> cls) {
    return classBindings.get(cls);
  }

  public Set<Class<?>> getInjectables() {
    return Collections.unmodifiableSet(classBindings.keySet());
  }

  public Set<Class<?>> resolve(Key key) {
    Set<Class<?>> matches = beanDefinitions.get(key.getType());

    if(matches == null) {
      return Collections.emptySet();
    }

    matches = new HashSet<>(matches);  // Make a copy as otherwise retainAll below will modify the beanDefinitions map

    for(Annotation qualifier : key.getQualifiers()) {
      Set<Class<?>> qualifierMatches = beanDefinitions.get(qualifier);

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

  public Map<AccessibleObject, Binding> put(Class<?> concreteClass) {
    if(concreteClass.isInterface()) {
      throw new IllegalArgumentException("parameter 'concreteClass' must be a concrete class: " + concreteClass);
    }
    if(classBindings.containsKey(concreteClass)) {
      throw new DuplicateBeanException(concreteClass);
    }

    Set<Annotation> qualifiers = extractQualifiers(concreteClass);
    Map<AccessibleObject, Binding> bindings = binder.resolve(concreteClass);

    policy.checkAddition(concreteClass, qualifiers, bindings);

    classBindings.put(concreteClass, bindings);

    for(Annotation annotation : qualifiers) {
      register(annotation, concreteClass);
    }
    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      register(cls, concreteClass);
    }

    return bindings;
  }

  public Map<AccessibleObject, Binding> remove(Class<?> concreteClass) {
    if(concreteClass.isInterface()) {
      throw new IllegalArgumentException("parameter 'concreteClass' must be a concrete class: " + concreteClass);
    }
    if(!classBindings.containsKey(concreteClass)) {
      throw new NoSuchBeanException(concreteClass);
    }

    Set<Annotation> qualifiers = extractQualifiers(concreteClass);  // TODO extractQualifiers might simply add ConcreteClass to the set?

    policy.checkRemoval(concreteClass, qualifiers);

    for(Annotation annotation : qualifiers) {
      remove(annotation, concreteClass);
    }
    for(Class<?> cls : getSuperClassesAndInterfaces(concreteClass)) {
      remove(cls, concreteClass);
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

  private void register(Object typeOrQualifier, Class<?> cls) {
    Set<Class<?>> concreteClasses = beanDefinitions.get(typeOrQualifier);

    if(concreteClasses == null) {
      concreteClasses = new HashSet<>();
      beanDefinitions.put(typeOrQualifier, concreteClasses);
    }

    if(!concreteClasses.add(cls)) {
      throw new AssertionError("Map 'beanDefinitions' already contained: " + cls + " for key: " + typeOrQualifier);
    }
  }

  private void remove(Object typeOrQualifier, Class<?> cls) {
    Set<Class<?>> concreteClasses = beanDefinitions.get(typeOrQualifier);

    if(concreteClasses == null || !concreteClasses.remove(cls)) {
      throw new AssertionError("Map 'beanDefinitions' must contain: " + cls + " for key: " + typeOrQualifier + " concreteClasses = " + concreteClasses);
    }

    if(concreteClasses.isEmpty()) {
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
}
