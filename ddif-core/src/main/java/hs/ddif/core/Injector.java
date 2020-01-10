package hs.ddif.core;

import hs.ddif.core.store.DiscoveryPolicy;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.inject.Provider;
import javax.inject.Singleton;

// TODO JSR-330: Named without value is treated differently ... some use field name, others default to empty?
public class Injector {

  /**
   * The store consistency policy this injector uses.
   */
  private final InjectorStoreConsistencyPolicy consistencyPolicy;

  /**
   * InjectableStore used by this Injector.  The Injector will safeguard that this store
   * only contains injectables that can be fully resolved.
   */
  private final InjectableStore<ScopedInjectable> store;

  /**
   * Map containing {@link ScopeResolver}s this injector can use.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopesResolversByAnnotation = new HashMap<>();

  public Injector(DiscoveryPolicy<ScopedInjectable> discoveryPolicy, ScopeResolver... scopeResolvers) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopesResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
    }

    scopesResolversByAnnotation.put(Singleton.class, new ScopeResolver() {
      private final Map<Class<?>, WeakReference<Object>> singletons = new WeakHashMap<>();

      @Override
      public <T> T get(Class<?> injectableClass) {
        WeakReference<Object> reference = singletons.get(injectableClass);

        if(reference != null) {
          @SuppressWarnings("unchecked")
          T bean = (T)reference.get();

          return bean;  // This may still return null
        }

        return null;
      }

      @Override
      public <T> void put(Class<?> injectableClass, T instance) {
        singletons.put(injectableClass, new WeakReference<Object>(instance));
      }

      @Override
      public Class<? extends Annotation> getScopeAnnotationClass() {
        return Singleton.class;
      }
    });

    this.consistencyPolicy = new InjectorStoreConsistencyPolicy();
    this.store = new InjectableStore<>(consistencyPolicy, discoveryPolicy);
  }

  public Injector(ScopeResolver... scopeResolvers) {
    this(null, scopeResolvers);
  }

  public Injector() {
    this((DiscoveryPolicy<ScopedInjectable>)null);
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param type the type of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws NoSuchBeanException when the given class is not registered with this Injector or the bean cannot be provided
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getInstance(Type type, Object... criteria) {
    Set<ScopedInjectable> injectables = store.resolve(type, criteria);

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
   * @param cls the class of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws NoSuchBeanException when the given class is not registered with this Injector or the bean cannot be provided
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getInstance(Class<T> cls, Object... criteria) {  // The signature of this method closely matches the other getInstance method as Class implements Type, however, this method will auto-cast the result thanks to the type parameter
    return getInstance((Type)cls, criteria);
  }

  /**
   * Returns all instances of the given type matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param type the type of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   */
  public <T> Set<T> getInstances(Type type, Object... criteria) {
    Set<T> instances = new HashSet<>();

    for(ScopedInjectable injectable : store.resolve(type, criteria)) {
      T instance = getInstance(injectable);

      if(instance != null) {  // Providers are allowed to return null for optional dependencies, donot include those in set.
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

  private <T> T getInstance(ScopedInjectable injectable) {
    ScopeResolver scopeResolver = null;

    for(Map.Entry<Class<? extends Annotation>, ScopeResolver> entry : scopesResolversByAnnotation.entrySet()) {
      if(injectable.getInjectableClass().getAnnotation(entry.getKey()) != null) {
        scopeResolver = entry.getValue();
        break;  // There can only ever be one scope match as multiple scope annotations are not allowed by ClassInjectable
      }
    }

    if(scopeResolver != null) {
      T bean = scopeResolver.get(injectable.getInjectableClass());

      if(bean != null) {
        return bean;
      }
    }

    @SuppressWarnings("unchecked")
    T bean = (T)injectable.getInstance(this);

    if(bean != null && scopeResolver != null) {

      /*
       * Store the result if scoped.
       */

      scopeResolver.put(injectable.getInjectableClass(), bean);
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
   * classes, then this method will throw an exception.<p>
   *
   * Note that if the given class implements {@link Provider} that
   * the class it provides is held to the same restrictions or registration
   * will fail.
   *
   * @param concreteClass the class to register with the Injector
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvableDependencyException when one or more dependencies of the given class cannot be resolved
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
   * classes, then this method will throw an exception.<p>
   *
   * Note that if the provided class implements {@link Provider} that
   * the class it provides is held to the same restrictions or registration
   * will fail.
   *
   * @param provider the provider to register with the Injector
   * @param qualifiers the qualifiers for this provider
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   * @throws UnresolvableDependencyException when one or more dependencies of the given provider cannot be resolved
   */
  public void register(Provider<?> provider, AnnotationDescriptor... qualifiers) {
    register(new ProvidedInjectable(provider, qualifiers));
  }

  /**
   * Registers an instance with this Injector as a Singleton if it would not
   * cause existing registered classes to have ambigious dependencies as a
   * result.<p>
   *
   * If registering this instance would result in ambigious dependencies for
   * previously registered classes, then this method will throw an exception.<p>
   *
   * Note that if the instance implements {@link Provider} that the class it
   * provides is held to the same restrictions or registration will fail.
   *
   * @param provider the provider to register with the Injector
   * @param qualifiers the qualifiers for this provider
   * @throws ViolatesSingularDependencyException when the registration would cause an ambigious dependency in one or more previously registered classes
   */
  public void registerInstance(Object instance, AnnotationDescriptor... qualifiers) {
    register(new InstanceInjectable(instance, qualifiers));
  }

  private void register(ScopedInjectable injectable) {
    store.put(injectable);

    for(Binding[] bindings : injectable.getBindings().values()) {
      for(Binding binding : bindings) {
        if(binding.getRequiredKey() != null) {
          consistencyPolicy.addReference(binding.getRequiredKey());
        }
      }
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
   * @param provider the provider to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Provider<?> provider) {
    remove(new ProvidedInjectable(provider));
  }

  private void remove(ScopedInjectable injectable) {
    store.remove(injectable);

    for(Binding[] bindings : injectable.getBindings().values()) {
      for(Binding binding : bindings) {
        if(binding.getRequiredKey() != null) {
          consistencyPolicy.removeReference(binding.getRequiredKey());
        }
      }
    }
  }
}
