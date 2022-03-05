package hs.ddif.core.config.standard;

import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.inject.store.BoundInstantiatorProvider;
import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.instantiation.injection.Injection;
import hs.ddif.core.scope.InjectionContext;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.Key;
import hs.ddif.core.store.Resolver;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Default implementation of an {@link InstantiationContext}.
 */
public class DefaultInstantiationContext implements InstantiationContext {
  private static final Logger LOGGER = Logger.getLogger(DefaultInstantiationContext.class.getName());

  /**
   * Stack of {@link InjectionContext}s used during the recursive creation of an
   * {@link Injectable}. The contexts refer to a parent if the injectable involved
   * is dependent scoped; any injectables added to a context, in order to be destroyed when
   * the context gets released, are added to top most ancestor.
   *
   * <p>Note that only a single thread should make use of this class at the same time.
   */
  private final Deque<LazyInjectionContext> stack = new ArrayDeque<>();

  private final Resolver<Injectable<?>> resolver;
  private final BoundInstantiatorProvider boundInstantiatorProvider;

  /**
   * Constructs a new instance.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param boundInstantiatorProvider an {@link BoundInstantiatorProvider}, cannot be {@code null}
   */
  public DefaultInstantiationContext(Resolver<Injectable<?>> resolver, BoundInstantiatorProvider boundInstantiatorProvider) {
    this.resolver = resolver;
    this.boundInstantiatorProvider = boundInstantiatorProvider;
  }

  @Override
  public synchronized <T> T create(Key key) throws InstanceCreationFailure, MultipleInstances {
    @SuppressWarnings("unchecked")
    Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

    if(injectables.size() > 1) {
      throw new MultipleInstances(key, injectables);
    }

    if(injectables.size() == 0) {
      return null;
    }

    return createInstance(injectables.iterator().next());
  }

  @Override
  public synchronized <T> List<T> createAll(Key key, Predicate<Type> typePredicate) throws InstanceCreationFailure {
    List<T> instances = new ArrayList<>();

    @SuppressWarnings("unchecked")
    Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

    for(Injectable<T> injectable : injectables) {
      if(typePredicate == null || typePredicate.test(injectable.getType())) {
        T instance = createInstanceInScope(injectable);

        if(instance != null) {
          instances.add(instance);
        }
      }
    }

    return instances;
  }

  private <T> T createInstance(Injectable<T> injectable) throws InstanceCreationFailure {
    return createInstance(injectable, false);
  }

  private <T> T createInstanceInScope(Injectable<T> injectable) throws InstanceCreationFailure {
    return createInstance(injectable, true);
  }

  private <T> T createInstance(Injectable<T> injectable, boolean allowOutOfScope) throws InstanceCreationFailure {
    ScopeResolver scopeResolver = injectable.getScopeResolver();

    try {
      if(!allowOutOfScope || scopeResolver.isScopeActive()) {
        LazyInjectionContext lazyInjectionContext = new LazyInjectionContext(stack.isEmpty() ? null : stack.getLast(), injectable);

        stack.addLast(lazyInjectionContext);

        try {
          T instance = scopeResolver.get(injectable, lazyInjectionContext);

          lazyInjectionContext.add(injectable, instance);

          return instance;
        }
        finally {
          stack.removeLast();
        }
      }

      return null;
    }
    catch(OutOfScopeException e) {
      if(!allowOutOfScope) {
        throw new InstanceCreationFailure(injectable.getType(), "could not be created", e);
      }

      /*
       * Scope was checked to be active (to avoid exception cost), but it still occurred...
       */

      LOGGER.warning("Scope " + scopeResolver.getScopeAnnotationClass() + " should have been active: " + e.getMessage());

      return null;  // same as if scope hadn't been active in the first place
    }
    catch(InstanceCreationFailure e) {
      throw e;
    }
    catch(Exception e) {
      throw new InstanceCreationFailure(injectable.getType(), "could not be created", e);
    }
  }

  /*
   * Possibly problematic gap in current implementation:
   *
   * This framework allows injection of optional dependencies and also collections.
   * When a dependency is not a hard required at any injection point it can be removed
   * from the injector at any time, even when still injected in an active object, without
   * failing any consistency checks.
   *
   * When an injectable is removed, it is removed from its corresponding scope resolver,
   * which in turn will release the associated InjectionContext, which in turn causes
   * the instance to be destroyed calling its destroy life cycle callbacks.
   *
   * This could potentially call destroy life cycle methods on an instance that is still
   * injected in an active object.
   *
   * Example:
   *
   * A singleton S has an optional dependency on a singleton T. When S is created, T
   * was available and is injected. When T is removed from the injector (possible if
   * there are only optional references to T) its destroy life cycle methods will get
   * called. This could potentially put T in a state where it can no longer be used.
   * Any method calls by S on T after its destruction may therefore have unexpected
   * results.
   */

  /**
   * A lazy implementation of an {@link InjectionContext} which only creates the list
   * of {@link Injection}s when first queried.
   *
   * <p>An injection context is created whenever an {@link Injectable} is obtained from
   * a {@link ScopeResolver}. If the resolver decides to create a new instance, it will
   * call recursively into a {@link InstantiationContext} (implemented by the outer
   * class).
   *
   * <p>Any injectable created recursively will have its own injection context which
   * is linked to a parent injection context IF the {@link Injectable} is of dependent
   * scope.
   *
   * <p>The current injectable context active for the {@link InstantiationContext} will
   * get any {@link Injectable}s added to it that were created. If the context has a
   * parent, the {@link Injectable}s will instead be added to its parent (or its parent
   * and so on).
   *
   * <p>In this way the injection contexts will keep track of a list of dependents that
   * should be destroyed when {@link InjectionContext#release()} is called.
   */
  private class LazyInjectionContext implements InjectionContext {
    private final Injectable<?> injectable;
    private final LazyInjectionContext parent;
    private final Deque<Runnable> dependents = new ArrayDeque<>();

    private List<Injection> injections;

    LazyInjectionContext(LazyInjectionContext parent, Injectable<?> injectable) {
      boolean dependent = injectable.getScopeResolver().getScopeAnnotationClass() == null;

      this.parent = dependent ? parent : null;
      this.injectable = injectable;
    }

    @Override
    public List<Injection> getInjections() throws InstanceCreationFailure, MultipleInstances, NoSuchInstance {
      if(injections == null) {
        List<Injection> injections = new ArrayList<>();

        for(Binding binding : injectable.getBindings()) {
          Instantiator<?> instantiator = boundInstantiatorProvider.getInstantiator(binding);

          injections.add(new Injection(binding.getAccessibleObject(), instantiator.getInstance(DefaultInstantiationContext.this)));
        }

        this.injections = injections;
      }

      return injections;
    }

    @Override
    public void release() {
      dependents.forEach(Runnable::run);
    }

    /**
     * Adds the given {@link Injectable} and its associated instance to this context
     * or its oldest known ancestor if it has a parent.
     *
     * @param <T> type of the instance produced by the {@link Injectable}
     * @param injectable an {@link Injectable}, cannot be {@code null}
     * @param instance an instance, cannot be {@code null}
     */
    <T> void add(Injectable<T> injectable, T instance) {
      if(parent == null) {
        dependents.addFirst(() -> injectable.destroy(instance));
      }
      else {
        parent.add(injectable, instance);
      }
    }
  }
}
