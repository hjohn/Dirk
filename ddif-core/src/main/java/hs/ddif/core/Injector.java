package hs.ddif.core;

import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.inject.Provider;
import javax.inject.Singleton;

// TODO JSR-330: Named without value is treated differently ... some use field name, others default to empty?
// TODO Binder class is pretty much static, and also has several public statics being used -- it is more of a utility class, consider refactoring
public class Injector {

  /**
   * The store consistency policy this injector uses.
   */
  private final InjectorStoreConsistencyPolicy consistencyPolicy = new InjectorStoreConsistencyPolicy();

  /**
   * InjectableStore used by this Injector.  The Injector will safeguard that this store
   * only contains injectables that can be fully resolved.
   */
  private final InjectableStore store;

  /**
   * Map containing known singleton instances.  Both key and value are weak to prevent the
   * injector from holding on to unused singletons, or to classes that could be unloaded.<p>
   *
   * The key is always a concrete class.
   */
  private final Map<Class<?>, WeakReference<Object>> singletons = new WeakHashMap<>();

  public Injector(DiscoveryPolicy discoveryPolicy) {
    this.store = new InjectableStore(consistencyPolicy, discoveryPolicy);
  }

  public Injector() {
    this(null);
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param type the type of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws NoSuchBeanException when the given class is not registered with this Injector or the bean cannot be provided
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getInstance(Type type, Object... criteria) {
    Set<Injectable> injectables = store.resolve(type, criteria);

    if(injectables.isEmpty()) {
      throw new NoSuchBeanException(type, criteria);
    }
    if(injectables.size() > 1) {
      throw new AmbigiousBeanException(injectables, type, criteria);
    }

    T instance;

    try {
      instance = getInstance(injectables.iterator().next());
    }
    catch(NoSuchBeanException e) {
      throw new NoSuchBeanException(type, e, criteria);
    }

    if(instance == null) {
      throw new NoSuchBeanException(type, criteria);
    }

    return instance;
  }

  /**
   * Returns an instance of the given class matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param type the class of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws NoSuchBeanException when the given class is not registered with this Injector or the bean cannot be provided
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getInstance(Class<T> cls, Object... criteria) {
    return getInstance((Type)cls, criteria);
  }

  /**
   * Returns all instances of the given type matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param cls the type of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   */
  public <T> Set<T> getInstances(Type type, Object... criteria) {
    Set<T> instances = new HashSet<>();

    for(Injectable injectable : store.resolve(type, criteria)) {
      T instance = getInstance(injectable);

      if(instance != null) {
        instances.add(instance);
      }
    }

    return instances;
  }

  /**
   * Returns all instances of the given class matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param cls the class of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   */
  public <T> Set<T> getInstances(Class<T> type, Object... criteria) {
    return getInstances((Type)type, criteria);
  }

  /**
   * Returns <code>true</code> when the given class is part of this Injector, otherwise
   * <code>false</code>.
   *
   * @param cls a class to check
   * @return <code>true</code> when the given class is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Class<?> cls) {
    return store.contains(cls);
  }

  protected <T> T getInstance(Injectable injectable) {
    boolean isSingleton = injectable.getInjectableClass().getAnnotation(Singleton.class) != null;

    if(isSingleton) {
      WeakReference<Object> reference = singletons.get(injectable.getInjectableClass());

      if(reference != null) {
        @SuppressWarnings("unchecked")
        T bean = (T)reference.get();  // create strong reference first

        if(bean != null) {  // if it was not null, return it
          return bean;
        }
      }
    }

    @SuppressWarnings("unchecked")
    T bean = (T)injectable.getInstance(this, store.getBindings(injectable.getInjectableClass()));

    /*
     * Store the result if singleton.
     */

    if(isSingleton) {
      singletons.put(injectable.getInjectableClass(), new WeakReference<Object>(bean));
    }

    return bean;
  }

  /**
   * Registers a class with this Injector if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given class.<p>
   *
   * If there are unresolvable dependencies, or registering this class
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.
   *
   * @param concreteClass the class to register with the Injector
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvedDependencyException when one or more dependencies of the given class cannot be resolved
   */
  public void register(Class<?> concreteClass) {
    register(new ClassInjectable(concreteClass));
  }

  /**
   * Registers a provider with this Injector if all its dependencies can be
   * resolved and it would not cause existing registered classes to have
   * ambigious dependencies as a result of registering the given provider.<p>
   *
   * If there are unresolvable dependencies, or registering this provider
   * would result in ambigious dependencies for previously registered
   * classes, then this method will throw an exception.
   *
   * @param provider the provider to register with the Injector
   * @param qualifiers the qualifiers for this provider
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvedDependencyException when one or more dependencies of the given provider cannot be resolved
   */
  public void register(Provider<?> provider, AnnotationDescriptor... qualifiers) {
    register(new ProvidedInjectable(provider, qualifiers));
  }

  public void registerInstance(Object instance, AnnotationDescriptor... qualifiers) {
    register(new InstanceInjectable(instance, qualifiers));
  }

  private void register(Injectable injectable) {
    Map<AccessibleObject, Binding> bindings = store.put(injectable);

    for(Binding binding : bindings.values()) {
      consistencyPolicy.addReferences(binding.getRequiredKeys());
    }
  }

  /**
   * Removes a class from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param concreteClass the class to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Class<?> concreteClass) {
    remove(new ClassInjectable(concreteClass));
  }

  /**
   * Removes a provider from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.
   *
   * @param concreteClass the class to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Provider<?> provider) {
    remove(new ProvidedInjectable(provider));
  }

  private void remove(Injectable injectable) {
    Map<AccessibleObject, Binding> bindings = store.remove(injectable);

    for(Binding binding : bindings.values()) {
      consistencyPolicy.removeReferences(binding.getRequiredKeys());
    }
  }
}
