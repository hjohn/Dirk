package hs.ddif.core.inject.instantiator;

import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.InjectableStore;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.apache.commons.lang3.reflect.TypeUtils;

/**
 * Supplies fully injected classes from the supplied store (usually managed by an
 * Injector).  The instances are returned from cache or created as needed.
 */
public class Instantiator {
  private static final Logger LOGGER = Logger.getLogger(Instantiator.class.getName());
  private static final ReferenceQueue<Object> REFERENCE_QUEUE = new ReferenceQueue<>();
  private static final NamedParameter[] NO_PARAMETERS = new NamedParameter[] {};
  private static final Thread WEAK_SINGLETON_CLEAN_WARNER;

  static {
    WEAK_SINGLETON_CLEAN_WARNER = new Thread(() -> {
      for(;;) {
        try {
          Reference<? extends Object> ref = REFERENCE_QUEUE.remove();

          LOGGER.info("Weak Singleton was garbage collected as it was no longer referenced: " + ref);
        }
        catch(InterruptedException e) {
          // ignore
        }
      }
    });

    WEAK_SINGLETON_CLEAN_WARNER.setDaemon(true);
    WEAK_SINGLETON_CLEAN_WARNER.start();
  }

  private final InjectableStore<ResolvableInjectable> store;
  private final InjectableDiscoverer discoverer;

  /**
   * Map containing {@link ScopeResolver}s this injector can use.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopesResolversByAnnotation = new HashMap<>();

  public Instantiator(InjectableStore<ResolvableInjectable> store, InjectableDiscoverer discoverer, ScopeResolver... scopeResolvers) {
    this.store = store;
    this.discoverer = discoverer;

    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopesResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
    }

    scopesResolversByAnnotation.put(Singleton.class, new ScopeResolver() {
      private final Map<Type, Object> singletons = new WeakHashMap<>();

      @Override
      public <T> T get(Type injectableType) {
        @SuppressWarnings("unchecked")
        T singleton = (T)singletons.get(injectableType);

        return singleton;
      }

      @Override
      public <T> void put(Type injectableType, T instance) {
        singletons.put(injectableType, instance);
      }

      @Override
      public Class<? extends Annotation> getScopeAnnotationClass() {
        return Singleton.class;
      }
    });

    scopesResolversByAnnotation.put(WeakSingleton.class, new ScopeResolver() {
      private final Map<Type, InformationalWeakReference<Object>> singletons = new WeakHashMap<>();

      @Override
      public <T> T get(Type injectableType) {
        InformationalWeakReference<Object> reference = singletons.get(injectableType);

        if(reference != null) {
          @SuppressWarnings("unchecked")
          T bean = (T)reference.get();

          return bean;  // This may still return null
        }

        return null;
      }

      @Override
      public <T> void put(Type injectableType, T instance) {
        singletons.put(injectableType, new InformationalWeakReference<>(instance, REFERENCE_QUEUE));
      }

      @Override
      public Class<? extends Annotation> getScopeAnnotationClass() {
        return WeakSingleton.class;
      }
    });
  }

  private static class InformationalWeakReference<T> extends WeakReference<T> {
    private final String info;

    public InformationalWeakReference(T referent, ReferenceQueue<T> queue) {
      super(referent, queue);

      this.info = referent.toString();
    }

    @Override
    public String toString() {
      return info;
    }
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies and parameters are injected.
   *
   * @param <T> the type of the instance
   * @param type the type of the instance required
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public synchronized <T> T getParameterizedInstance(Type type, NamedParameter[] parameters, Object... criteria) throws BeanResolutionException {
    Set<ResolvableInjectable> injectables = resolve(type, criteria);

    if(injectables.isEmpty()) {
      throw new BeanResolutionException(type, criteria);
    }
    if(injectables.size() > 1) {
      throw new BeanResolutionException(injectables, type, criteria);
    }

    T instance;

    try {
      instance = getInstance(injectables.iterator().next(), parameters);
    }
    catch(BeanResolutionException | OutOfScopeException e) {
      throw new BeanResolutionException(type, e, criteria);
    }

    if(instance == null) {
      throw new BeanResolutionException(type, criteria);
    }

    return instance;
  }

  /**
   * Returns an instance of the given type matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param type the type of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria, never null
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public synchronized <T> T getInstance(Type type, Object... criteria) throws BeanResolutionException {
    return getParameterizedInstance(type, NO_PARAMETERS, criteria);
  }

  /**
   * Returns an instance of the given class matching the given criteria (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param cls the class of the instance required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return an instance of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when the given class is not registered with this Injector or the bean cannot be provided
   *   or when the given class has multiple matching candidates
   */
  public synchronized <T> T getInstance(Class<T> cls, Object... criteria) throws BeanResolutionException {  // The signature of this method closely matches the other getInstance method as Class implements Type, however, this method will auto-cast the result thanks to the type parameter
    return getInstance((Type)cls, criteria);
  }

  /**
   * Returns all instances of the given type matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param <T> the type of the instance
   * @param type the type of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when a required bean could not be found
   */
  public synchronized <T> List<T> getInstances(Type type, Object... criteria) throws BeanResolutionException {
    List<T> instances = new ArrayList<>();

    for(ResolvableInjectable injectable : resolve(type, criteria)) {
      try {
        T instance = getInstance(injectable, NO_PARAMETERS);

        if(instance != null) {  // Providers are allowed to return null for optional dependencies, donot include those in set.
          instances.add(instance);
        }
      }
      catch(OutOfScopeException e) {
        throw new BeanResolutionException(type, e, criteria);
      }
    }

    return instances;
  }

  /**
   * Returns all instances of the given class matching the given criteria (if any) in
   * which all dependencies are injected.  When there are no matches, an empty set is
   * returned.
   *
   * @param <T> the type of the instances
   * @param cls the class of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when a required bean could not be found
   */
  public synchronized <T> List<T> getInstances(Class<T> cls, Object... criteria) throws BeanResolutionException {
    return getInstances((Type)cls, criteria);
  }

  private Set<ResolvableInjectable> resolve(Type type, Object... criteria) throws BeanResolutionException {
    Set<ResolvableInjectable> injectables = store.resolve(type, criteria);

    if(injectables.isEmpty() && discoverer != null && criteria.length == 0) {
      try {
        Class<?> typeClass = TypeUtils.getRawType(type, null);
        List<ResolvableInjectable> discovered = discoverer.discover(store, typeClass);

        if(!discovered.isEmpty()) {
          store.putAll(discovered);

          return store.resolve(type, criteria);
        }
      }
      catch(DiscoveryException e) {
        throw new BeanResolutionException(type, e, criteria);
      }
    }

    return injectables;
  }

  private <T> T getInstance(ResolvableInjectable injectable, NamedParameter[] namedParameters) throws BeanResolutionException, OutOfScopeException {
    ScopeResolver scopeResolver = scopesResolversByAnnotation.get(injectable.getScope() == null ? null : injectable.getScope().annotationType());

    if(scopeResolver != null) {
      T bean = scopeResolver.get(injectable.getType());

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

      scopeResolver.put(injectable.getType(), bean);
    }

    return bean;
  }
}
