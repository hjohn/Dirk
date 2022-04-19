package hs.ddif.core;

import hs.ddif.api.instantiation.InstantiationContext;
import hs.ddif.api.instantiation.Instantiator;
import hs.ddif.api.instantiation.domain.InstanceCreationException;
import hs.ddif.api.instantiation.domain.Key;
import hs.ddif.api.instantiation.domain.MultipleInstancesException;
import hs.ddif.api.instantiation.domain.NoSuchInstanceException;
import hs.ddif.api.scope.CreationalContext;
import hs.ddif.api.scope.OutOfScopeException;
import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.core.definition.Binding;
import hs.ddif.core.definition.Injectable;
import hs.ddif.core.definition.injection.Injection;
import hs.ddif.core.inject.store.BoundInstantiatorProvider;
import hs.ddif.core.store.Resolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Default implementation of an {@link InstantiationContext}.
 */
class DefaultInstantiationContext implements InstantiationContext {
  private static final Logger LOGGER = Logger.getLogger(DefaultInstantiationContext.class.getName());

  /**
   * Stack of {@link CreationalContext}s used during the recursive creation of an
   * {@link Injectable}. The contexts refer to a parent if the injectable involved
   * is dependent scoped; any injectables added to a context, in order to be destroyed when
   * the context gets released, are added to top most ancestor.
   *
   * <p>Note that only a single thread should make use of this class at the same time.
   */
  private final Deque<LazyCreationalContext<?>> stack = new ArrayDeque<>();

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
  public synchronized <T> T create(Key key) throws InstanceCreationException, MultipleInstancesException {
    @SuppressWarnings("unchecked")
    Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

    if(injectables.size() > 1) {
      throw new MultipleInstancesException(key, injectables);
    }

    if(injectables.size() == 0) {
      return null;
    }

    return createInstance(injectables.iterator().next());
  }

  @Override
  public synchronized <T> List<T> createAll(Key key) throws InstanceCreationException {
    List<T> instances = new ArrayList<>();

    @SuppressWarnings("unchecked")
    Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

    for(Injectable<T> injectable : injectables) {
      T instance = createInstanceInScope(injectable);

      if(instance != null) {
        instances.add(instance);
      }
    }

    return instances;
  }

  private <T> T createInstance(Injectable<T> injectable) throws InstanceCreationException {
    return createInstance(injectable, false);
  }

  private <T> T createInstanceInScope(Injectable<T> injectable) throws InstanceCreationException {
    return createInstance(injectable, true);
  }

  private <T> T createInstance(Injectable<T> injectable, boolean allowOutOfScope) throws InstanceCreationException {
    ScopeResolver scopeResolver = injectable.getScopeResolver();

    try {
      if(!allowOutOfScope || scopeResolver.isActive()) {
        LazyCreationalContext<T> lazyCreationalContext = new LazyCreationalContext<>(stack.isEmpty() ? null : stack.getLast(), injectable);

        stack.addLast(lazyCreationalContext);

        try {
          T instance = scopeResolver.get(injectable, lazyCreationalContext);

          lazyCreationalContext.add(injectable, instance);

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
        throw new InstanceCreationException(injectable.getType(), "could not be created", e);
      }

      /*
       * Scope was checked to be active (to avoid exception cost), but it still occurred...
       */

      LOGGER.warning("Scope " + scopeResolver.getAnnotationClass() + " should have been active: " + e.getMessage());

      return null;  // same as if scope hadn't been active in the first place
    }
    catch(InstanceCreationException e) {
      throw e;
    }
    catch(Exception e) {
      throw new InstanceCreationException(injectable.getType(), "could not be created", e);
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
   * A lazy implementation of a {@link CreationalContext} which only creates the list
   * of {@link Injection}s when first queried.
   *
   * <p>A {@code CreationalContext} is created whenever an {@link Injectable} is obtained from
   * a {@link ScopeResolver}. If the resolver decides to create a new instance, it will
   * call recursively into a {@link InstantiationContext} (implemented by the outer
   * class).
   *
   * <p>Any injectable created recursively will have its own context which
   * is linked to a parent context IF the {@link Injectable} is of dependent
   * scope.
   *
   * <p>The current context active for the {@link InstantiationContext} will
   * get any {@link Injectable}s added to it that were created. If the context has a
   * parent, the {@link Injectable}s will instead be added to its parent (or its parent
   * and so on).
   *
   * <p>In this way the contexts will keep track of a list of dependents that
   * should be destroyed when {@link CreationalContext#release()} is called.
   */
  private class LazyCreationalContext<T> implements CreationalContext<T> {
    private final Injectable<T> injectable;
    private final LazyCreationalContext<?> parent;

    private Deque<Runnable> dependents = new ArrayDeque<>();
    private List<Injection> injections;

    LazyCreationalContext(LazyCreationalContext<?> parent, Injectable<T> injectable) {
      this.parent = injectable.getScopeResolver().isDependentScope() ? parent : null;
      this.injectable = injectable;
    }

    @Override
    public Reference<T> create() throws InstanceCreationException, MultipleInstancesException, NoSuchInstanceException {
      return new LazyReference<>(this, injectable.create(getInjections()));
    }

    private List<Injection> getInjections() throws InstanceCreationException, MultipleInstancesException, NoSuchInstanceException {
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

    /**
     * Adds the given {@link Injectable} and its associated instance to this context
     * or its oldest known ancestor if it has a parent.
     *
     * @param <U> type of the instance produced by the {@link Injectable}
     * @param injectable an {@link Injectable}, cannot be {@code null}
     * @param instance an instance, cannot be {@code null}
     */
    <U> void add(Injectable<U> injectable, U instance) {
      if(dependents == null) {
        throw new IllegalStateException("context was already released");
      }

      if(parent == null) {
        dependents.addFirst(() -> injectable.destroy(instance));
      }
      else {
        parent.add(injectable, instance);
      }
    }
  }

  private static class LazyReference<T> implements CreationalContext.Reference<T> {
    private final T instance;
    private final LazyCreationalContext<T> creationalContext;

    LazyReference(LazyCreationalContext<T> creationalContext, T instance) {
      this.instance = instance;
      this.creationalContext = creationalContext;
    }

    @Override
    public T get() {
      if(creationalContext.dependents == null) {
        throw new IllegalStateException("context was already released");
      }

      return instance;
    }

    @Override
    public void release() {
      if(creationalContext.dependents != null) {
        creationalContext.dependents.forEach(Runnable::run);
        creationalContext.dependents = null;
      }
    }
  }
}
