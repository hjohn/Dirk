package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.NamedParameter;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Criteria;
import hs.ddif.core.store.InjectableStore;
import hs.ddif.core.store.Key;

import java.lang.annotation.Annotation;
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

  /**
   * Map containing {@link ScopeResolver}s this injector can use.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopesResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param store a {@link InjectableStore}, cannot be null
   * @param gatherer a {@link Gatherer}, cannot be null
   * @param scopeResolvers an array of {@link ScopeResolver}s this instance should use
   */
  public Instantiator(InjectableStore<ResolvableInjectable> store, Gatherer gatherer, ScopeResolver... scopeResolvers) {
    this.store = store;
    this.gatherer = gatherer;

    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopesResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
    }
  }

  /**
   * Returns an instance matching the given {@link Key} and {@link Criteria} (if any) in
   * which all dependencies and parameters are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @param criteria a {@link Criteria} instance, cannot be null
   * @return an instance of the given class matching the given criteria, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T getInstance(Key key, NamedParameter[] parameters, Criteria criteria) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    T object = findInstance(key, parameters, criteria);

    if(object == null) {
      throw new NoSuchInstance(key, criteria);
    }

    return object;
  }

  /**
   * Returns an instance matching the given {@link Key} (if any) in
   * which all dependencies and parameters are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @return an instance matching the given {@link Key}, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T getInstance(Key key, NamedParameter[] parameters) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    return getInstance(key, parameters, Criteria.EMPTY);
  }

  /**
   * Finds an instance matching the given {@link Key} and {@link Criteria} (if any) in
   * which all dependencies and parameters are injected. If not found, {@code null}
   * is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param parameters an array of {@link NamedParameter}'s required for creating the given type, cannot be null
   * @param criteria a {@link Criteria} instance, cannot be null
   * @return an instance of the given class matching the given criteria, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T findInstance(Key key, NamedParameter[] parameters, Criteria criteria) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure {
    Set<ResolvableInjectable> injectables = discover(key, criteria);

    if(injectables.isEmpty()) {
      return null;
    }
    if(injectables.size() > 1) {
      throw new MultipleInstances(key, criteria, injectables);
    }

    ResolvableInjectable injectable = injectables.iterator().next();

    return getInstance(injectable, parameters, findScopeResolver(injectable));
  }

  /**
   * Returns an instance matching the given {@link Key} and {@link Criteria} (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param criteria a {@link Criteria} instance, cannot be null
   * @return an instance of the given class matching the given criteria, never null
   * @throws OutOfScopeException when out of scope
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T getInstance(Key key, Criteria criteria) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    return getInstance(key, NO_PARAMETERS, criteria);
  }

  /**
   * Returns an instance matching the given {@link Key} (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return an instance matching the given {@link Key}, never null
   * @throws OutOfScopeException when out of scope
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T getInstance(Key key) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    return getInstance(key, NO_PARAMETERS);
  }

  /**
   * Finds an instance matching the given {@link Key} (if any) in
   * which all dependencies are injected. If not found, {@code null}
   * is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return an instance matching the given {@link Key}, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> T findInstance(Key key) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure {
    return findInstance(key, NO_PARAMETERS, Criteria.EMPTY);
  }

  /**
   * Returns all instances matching the given {@link Key} and {@link Criteria} (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param criteria a {@link Criteria} instance, cannot be null
   * @return all instances of the given class matching the given criteria (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> List<T> getInstances(Key key, Criteria criteria) throws InstanceCreationFailure {
    List<T> instances = new ArrayList<>();

    for(ResolvableInjectable injectable : store.resolve(key, criteria)) {
      ScopeResolver scopeResolver = findScopeResolver(injectable);

      if(scopeResolver == null || scopeResolver.isScopeActive(injectable)) {
        try {
          T instance = getInstance(injectable, NO_PARAMETERS, scopeResolver);

          if(instance != null) {  // Providers are allowed to return null for optional dependencies, don't include those in set.
            instances.add(instance);
          }
        }
        catch(OutOfScopeException e) {

          /*
           * Scope was checked to be active (to avoid exception cost), but it still occurred...
           */

          throw new IllegalStateException("scope should have been active, concurrent modification on another thread?", e);
        }
      }
    }

    return instances;
  }

  /**
   * Returns all instances matching the given {@link Key} (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return all instances of the given class matching the given criteria (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  public synchronized <T> List<T> getInstances(Key key) throws InstanceCreationFailure {
    return getInstances(key, Criteria.EMPTY);
  }

  private Set<ResolvableInjectable> discover(Key key, Criteria criteria) throws DiscoveryFailure {
    Set<ResolvableInjectable> injectables = store.resolve(key, criteria);

    if(!injectables.isEmpty()) {
      return injectables;
    }

    if(!criteria.getInterfaces().isEmpty() || !criteria.getMatchers().isEmpty()) {
      return Set.of();
    }

    Set<ResolvableInjectable> gatheredInjectables = gatherer.gather(store, key);

    if(gatheredInjectables.isEmpty()) {
      return Set.of();
    }

    try {
      store.putAll(gatheredInjectables);

      return store.resolve(key);
    }
    catch(Exception e) {
      throw new DiscoveryFailure(key, "Exception while adding auto discovered injectables: " + gatheredInjectables + " to injector for", e);
    }
  }

  private <T> T getInstance(ResolvableInjectable injectable, NamedParameter[] namedParameters, ScopeResolver scopeResolver) throws InstanceCreationFailure, OutOfScopeException {
    if(scopeResolver != null) {
      T instance = scopeResolver.get(injectable);

      if(instance != null) {
        return instance;
      }
    }

    @SuppressWarnings("unchecked")
    T instance = (T)injectable.getObjectFactory().createInstance(this, namedParameters);

    if(instance != null && scopeResolver != null) {

      /*
       * Store the result if scoped.
       */

      scopeResolver.put(injectable, instance);
    }

    return instance;
  }

  private ScopeResolver findScopeResolver(ResolvableInjectable injectable) {
    Annotation scope = injectable.getScope();

    if(scope == null) {
      return null;
    }

    return scopesResolversByAnnotation.get(scope.annotationType());  // this may return null if scope is not known, a consistency policy should police this, not the instantiator
  }
}
