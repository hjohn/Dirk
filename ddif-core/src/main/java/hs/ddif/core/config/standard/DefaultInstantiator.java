package hs.ddif.core.config.standard;

import hs.ddif.core.config.gather.DiscoveryFailure;
import hs.ddif.core.config.gather.Gatherer;
import hs.ddif.core.inject.bind.Binding;
import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.inject.injection.Injection;
import hs.ddif.core.inject.instantiation.InstanceCreationFailure;
import hs.ddif.core.inject.instantiation.Instantiator;
import hs.ddif.core.inject.instantiation.MultipleInstances;
import hs.ddif.core.inject.instantiation.NoSuchInstance;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.QualifiedType;
import hs.ddif.core.store.QualifiedTypeStore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Implementation of {@link Instantiator}, which supplies fully injected classes from the supplied store
 * (usually managed by an Injector).  The instances are returned from cache or created as needed.
 */
public class DefaultInstantiator implements Instantiator {
  private static final ScopeResolver NULL_SCOPE_RESOLVER = new NullScopeResolver();

  private final QualifiedTypeStore<Injectable> store;
  private final Gatherer gatherer;

  /**
   * Map containing {@link ScopeResolver}s this injector can use.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopeResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param store a {@link QualifiedTypeStore}, cannot be null
   * @param gatherer a {@link Gatherer}, cannot be null
   * @param scopeResolvers an array of {@link ScopeResolver}s this instance should use
   */
  public DefaultInstantiator(QualifiedTypeStore<Injectable> store, Gatherer gatherer, ScopeResolver... scopeResolvers) {
    this.store = store;
    this.gatherer = gatherer;

    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopeResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
    }
  }

  /**
   * Returns an instance matching the given {@link Key} and a list of {@link Predicate}s (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return an instance of the given class matching the given matchers, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  @Override
  public synchronized <T> T getInstance(Key key, List<Predicate<Type>> matchers) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    T object = findInstance(key, matchers);

    if(object == null) {
      throw new NoSuchInstance(key, matchers);
    }

    return object;
  }

  /**
   * Returns an instance matching the given {@link Key} (if any) in
   * which all dependencies are injected.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @return an instance matching the given {@link Key}, never null
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  @Override
  public synchronized <T> T getInstance(Key key) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    return getInstance(key, List.of());
  }

  /**
   * Finds an instance matching the given {@link Key} and a list of {@link Predicate}s (if any) in
   * which all dependencies are injected. If not found, {@code null}
   * is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return an instance of the given class matching the given matchers, or {@code null} when no instance was found
   * @throws OutOfScopeException when out of scope
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  @Override
  public synchronized <T> T findInstance(Key key, List<Predicate<Type>> matchers) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure {
    Set<Injectable> injectables = discover(key, matchers);

    if(injectables.isEmpty()) {
      return null;
    }
    if(injectables.size() > 1) {
      throw new MultipleInstances(key, matchers, injectables);
    }

    Injectable injectable = injectables.iterator().next();

    return getInstance(injectable, findScopeResolver(injectable));
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
  @Override
  public synchronized <T> T findInstance(Key key) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure {
    return findInstance(key, List.of());
  }

  /**
   * Returns all instances matching the given {@link Key} and a list of {@link Predicate}s (if any) and, if scoped,
   * which are active in the current scope.  When there are no matches, an empty set is returned.
   *
   * @param <T> the type of the instance
   * @param key a {@link Key} identifying the type of the instance required, cannot be null
   * @param matchers a list of {@link Predicate}s, cannot be null
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  @Override
  public synchronized <T> List<T> getInstances(Key key, List<Predicate<Type>> matchers) throws InstanceCreationFailure {
    List<T> instances = new ArrayList<>();

    for(Injectable injectable : store.resolve(key, matchers)) {
      ScopeResolver scopeResolver = findScopeResolver(injectable);

      if(scopeResolver == null || scopeResolver.isScopeActive(injectable)) {
        try {
          T instance = getInstance(injectable, scopeResolver);

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
   * @return all instances of the given class matching the given matchers (if any)
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  @Override
  public synchronized <T> List<T> getInstances(Key key) throws InstanceCreationFailure {
    return getInstances(key, List.of());
  }

  private Set<Injectable> discover(Key key, List<Predicate<Type>> matchers) throws DiscoveryFailure {
    Set<Injectable> injectables = store.resolve(key, matchers);

    if(!injectables.isEmpty()) {
      return injectables;
    }

    if(!matchers.isEmpty()) {
      return Set.of();
    }

    Set<Injectable> gatheredInjectables = gatherer.gather(store, key);

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

  private <T> T getInstance(Injectable injectable, ScopeResolver scopeResolver) throws InstanceCreationFailure, OutOfScopeException {
    try {
      return scopeResolver.get(injectable, () -> createInstance(injectable));
    }
    catch(OutOfScopeException e) {
      throw e;
    }
    catch(Exception e) {
      if(e instanceof InstanceCreationFailure) {
        throw (InstanceCreationFailure)e;
      }

      throw new InstanceCreationFailure(injectable.getType(), "Exception while creating instance", e);
    }
  }

  private <T> T createInstance(Injectable injectable) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException {
    List<Injection> injections = new ArrayList<>();

    for(Binding binding : injectable.getBindings()) {
      injections.add(new Injection(binding.getAccessibleObject(), binding.getValue(this)));
    }

    @SuppressWarnings("unchecked")
    T instance = (T)injectable.createInstance(injections);

    return instance;
  }

  private ScopeResolver findScopeResolver(Injectable injectable) {
    Annotation scope = injectable.getScope();

    return scopeResolversByAnnotation.getOrDefault(scope == null ? null : scope.annotationType(), NULL_SCOPE_RESOLVER);  // this may return NULL_SCOPE_RESOLVER if scope is not known, a consistency policy should police this, not the instantiator
  }

  private static class NullScopeResolver implements ScopeResolver {
    @Override
    public Class<? extends Annotation> getScopeAnnotationClass() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isScopeActive(QualifiedType qualifiedType) {
      return true;
    }

    @Override
    public <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
      return objectFactory.call();
    }
  }
}
