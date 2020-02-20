package hs.ddif.core;

import hs.ddif.core.store.DiscoveryPolicy;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.util.AnnotationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.inject.Provider;
import javax.inject.Singleton;

// TODO JSR-330: Named without value is treated differently ... some use field name, others default to empty?
public class Injector {
  private static final NamedParameter[] NO_PARAMETERS = new NamedParameter[] {};

  /**
   * Allows simple extension to an {@link Injector}.
   */
  public interface Extension {

    /**
     * Gets another {@link ScopedInjectable} derived from the given injectable, or
     * <code>null</code> if no other injectable could be derived.<p>
     *
     * During this method call the supplied {@link Injector} should not be modified
     * as this method is called during modification of the injectors internal state.<p>
     *
     * @param injector an {@link Injector}, never null
     * @param injectable a {@link ScopedInjectable}, never null
     * @return another {@link ScopedInjectable} derived from the given injectable, or
     *   <code>null</code> if no other injectable could be derived
     */
    ScopedInjectable getDerived(Injector injector, ScopedInjectable injectable);
  }

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

  private final List<Extension> extensions;

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
    this.extensions = Arrays.asList(new Extension[] {new ProviderInjectorExtension(), new ProducerInjectorExtension()});
  }

  public Injector(ScopeResolver... scopeResolvers) {
    this(null, scopeResolvers);
  }

  public Injector() {
    this((DiscoveryPolicy<ScopedInjectable>)null);
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies and parameters are injected.
   *
   * @param type the type of the instance required
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws NoSuchBeanException when the given class is not registered with this Injector or the bean cannot be provided
   * @throws AmbigiousBeanException when the given class has multiple matching candidates
   */
  public <T> T getParameterizedInstance(Type type, NamedParameter[] parameters, Object... criteria) {
    Set<ScopedInjectable> injectables = store.resolve(type, criteria);

    if(injectables.isEmpty()) {
      throw new NoSuchBeanException(type, criteria);
    }
    if(injectables.size() > 1) {
      throw new AmbigiousBeanException(injectables, type, criteria);
    }

    T instance;

    try {
      instance = getInstance(injectables.iterator().next(), parameters);
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
    return getParameterizedInstance(type, NO_PARAMETERS, criteria);
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
      T instance = getInstance(injectable, NO_PARAMETERS);

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
   * @param cls a class to check for, cannot be null
   * @return <code>true</code> when the given class is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Class<?> cls) {
    return store.contains(cls);
  }

  /**
   * Returns <code>true</code> when the given type with the given criteria is part of this
   * Injector, otherwise <code>false</code>.
   *
   * @param type a type to check for, cannot be null
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Class, Object...)}
   * @return <code>true</code> when the given type with the given criteria is part of this Injector, otherwise <code>false</code>
   */
  public boolean contains(Type type, Object... criteria) {
    return store.contains(type, criteria);
  }

  private <T> T getInstance(ScopedInjectable injectable, NamedParameter[] namedParameters) {
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
    T bean = (T)injectable.getInstance(this, namedParameters);

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

  /**
   * Removes a class from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the class implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param concreteClass the class to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void remove(Class<?> concreteClass) {
    remove(new ClassInjectable(concreteClass));
  }

  /**
   * Removes an instance from this Injector if doing so would not result in
   * broken dependencies in the remaining registered classes.<p>
   *
   * If there would be broken dependencies then the removal will fail
   * and an exception is thrown.<p>
   *
   * Note that if the instance implements {@link Provider} that the class it
   * provides is held to the same restrictions or removal will fail.
   *
   * @param instance the instance to remove from the Injector
   * @throws ViolatesSingularDependencyException when the removal would cause a missing dependency in one or more of the remaining registered classes
   */
  public void removeInstance(Object instance) {
    remove(new InstanceInjectable(instance));
  }

  private void register(ScopedInjectable injectable) {
    registerSingle(injectable);

    List<ScopedInjectable> registered = new ArrayList<>();

    registered.add(injectable);

    for(Extension extension : extensions) {
      try {
        ScopedInjectable derived = extension.getDerived(this, injectable);

        if(derived != null) {
          register(derived);
          registered.add(derived);
        }
      }
      catch(Exception e) {
        for(int i = registered.size() - 1; i >= 0; i--) {
          removeSingle(registered.get(i));
        }

        throw e;
      }
    }
  }

  private void remove(ScopedInjectable injectable) {
    removeSingle(injectable);

    List<ScopedInjectable> removed = new ArrayList<>();

    removed.add(injectable);

    for(Extension extension : extensions) {
      try {
        ScopedInjectable derived = extension.getDerived(this, injectable);

        if(derived != null) {
          remove(derived);
          removed.add(derived);
        }
      }
      catch(Exception e) {
        for(int i = removed.size() - 1; i >= 0; i--) {
          registerSingle(removed.get(i));
        }

        throw e;
      }
    }
  }

  private void registerSingle(ScopedInjectable injectable) {
    store.put(injectable);

    for(Binding[] bindings : injectable.getBindings().values()) {
      for(Binding binding : bindings) {
        if(binding.getRequiredKey() != null) {
          consistencyPolicy.addReference(binding.getRequiredKey());
        }
      }
    }
  }

  private void removeSingle(ScopedInjectable injectable) {
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
