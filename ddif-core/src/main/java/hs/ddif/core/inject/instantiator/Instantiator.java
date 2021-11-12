package hs.ddif.core.inject.instantiator;

import hs.ddif.core.bind.NamedParameter;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.InjectableStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Supplies fully injected classes from the supplied store (usually managed by an
 * Injector).  The instances are returned from cache or created as needed.
 */
public class Instantiator {
  private static final NamedParameter[] NO_PARAMETERS = new NamedParameter[] {};

  private final InjectableStore<ResolvableInjectable> store;
  private final Gatherer gatherer;
  private final boolean autoDiscovery;

  /**
   * Map containing {@link ScopeResolver}s this injector can use.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopesResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param store a {@link InjectableStore}, cannot be null
   * @param gatherer a {@link Gatherer}, cannot be null
   * @param autoDiscovery {@code true} when injectables should be discovered if missing, otherwise set to {@code false}
   * @param scopeResolvers an array of {@link ScopeResolver}s this instance should use
   */
  public Instantiator(InjectableStore<ResolvableInjectable> store, Gatherer gatherer, boolean autoDiscovery, ScopeResolver... scopeResolvers) {
    this.store = store;
    this.gatherer = gatherer;
    this.autoDiscovery = autoDiscovery;

    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopesResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
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
    Set<ResolvableInjectable> injectables = discover(type, criteria);

    if(injectables.isEmpty()) {
      throw new BeanResolutionException(type, criteria);
    }
    if(injectables.size() > 1) {
      throw new BeanResolutionException(injectables, type, criteria);
    }

    try {
      ResolvableInjectable injectable = injectables.iterator().next();

      T instance = getInstance(injectable, parameters, findScopeResolver(injectable));

      if(instance == null) {
        throw new BeanResolutionException(type, criteria);
      }

      return instance;
    }
    catch(InstantiationException | OutOfScopeException e) {
      throw new BeanResolutionException(type, e, criteria);
    }
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
   * Returns all instances of the given type matching the given criteria (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param type the type of the instances required
   * @param criteria optional list of criteria, see {@link InjectableStore#resolve(Type, Object...)}
   * @return all instances of the given class matching the given criteria (if any)
   * @throws BeanResolutionException when a required bean could not be found
   */
  public synchronized <T> List<T> getInstances(Type type, Object... criteria) throws BeanResolutionException {
    try {
      List<T> instances = new ArrayList<>();

      for(ResolvableInjectable injectable : store.resolve(type, criteria)) {
        ScopeResolver scopeResolver = findScopeResolver(injectable);

        if(scopeResolver == null || scopeResolver.isScopeActive(injectable.getType())) {
          try {
            T instance = getInstance(injectable, NO_PARAMETERS, scopeResolver);

            if(instance != null) {  // Providers are allowed to return null for optional dependencies, donot include those in set.
              instances.add(instance);
            }
          }
          catch(OutOfScopeException e) {

            /*
             * Scope was checked to be active (to avoid exception cost), but it still occured...
             */

            throw new IllegalStateException("scope should have been active, concurrent modification on another thread?", e);
          }
        }
      }

      return instances;
    }
    catch(InstantiationException e) {
      throw new BeanResolutionException(type, e, criteria);
    }
  }

  /**
   * Returns all instances of the given class matching the given criteria (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
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

  private Set<ResolvableInjectable> discover(Type type, Object... criteria) throws BeanResolutionException {
    try {
      Set<ResolvableInjectable> injectables = store.resolve(type, criteria);

      if(injectables.isEmpty() && autoDiscovery && criteria.length == 0) {
        store.putAll(gatherer.gather(type));

        injectables = store.resolve(type, criteria);
      }

      return injectables;
    }
    catch(Exception e) {
      throw new BeanResolutionException(type, e, criteria);
    }
  }

  private <T> T getInstance(ResolvableInjectable injectable, NamedParameter[] namedParameters, ScopeResolver scopeResolver) throws InstantiationException, OutOfScopeException {
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

  private ScopeResolver findScopeResolver(ResolvableInjectable injectable) {
    return scopesResolversByAnnotation.get(injectable.getScope() == null ? null : injectable.getScope().annotationType());
  }
}
