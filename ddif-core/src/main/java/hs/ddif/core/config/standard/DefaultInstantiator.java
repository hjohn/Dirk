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
   * @param store a {@link QualifiedTypeStore}, cannot be {@code null}
   * @param gatherer a {@link Gatherer}, cannot be {@code null}
   * @param scopeResolvers an array of {@link ScopeResolver}s this instance should use
   */
  public DefaultInstantiator(QualifiedTypeStore<Injectable> store, Gatherer gatherer, ScopeResolver... scopeResolvers) {
    this.store = store;
    this.gatherer = gatherer;

    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopeResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
    }
  }

  @Override
  public synchronized <T> T getInstance(Key key) throws OutOfScopeException, NoSuchInstance, MultipleInstances, InstanceCreationFailure {
    T object = findInstance(key);

    if(object == null) {
      throw new NoSuchInstance(key);
    }

    return object;
  }

  @Override
  public synchronized <T> T findInstance(Key key) throws OutOfScopeException, MultipleInstances, InstanceCreationFailure {
    Set<Injectable> injectables = discover(key);

    if(injectables.isEmpty()) {
      return null;
    }
    if(injectables.size() > 1) {
      throw new MultipleInstances(key, injectables);
    }

    Injectable injectable = injectables.iterator().next();

    return getInstance(injectable, findScopeResolver(injectable));
  }

  @Override
  public synchronized <T> List<T> getInstances(Key key, Predicate<Type> typePredicate) throws InstanceCreationFailure {
    List<T> instances = new ArrayList<>();

    for(Injectable injectable : store.resolve(key, typePredicate)) {
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

  @Override
  public synchronized <T> List<T> getInstances(Key key) throws InstanceCreationFailure {
    return getInstances(key, null);
  }

  private Set<Injectable> discover(Key key) throws DiscoveryFailure {
    Set<Injectable> injectables = store.resolve(key);

    if(!injectables.isEmpty()) {
      return injectables;
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
