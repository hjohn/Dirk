package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.Key;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.store.Resolver;
import org.int4.dirk.org.apache.commons.lang3.reflect.TypeUtils;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.scope.CreationalContext;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Types;

/**
 * Factory for {@link InstantiationContext}s.
 */
class InstantiationContextFactory {
  private static final Logger LOGGER = Logger.getLogger(InstantiationContextFactory.class.getName());

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
  private final AnnotationStrategy annotationStrategy;
  private final ProxyStrategy proxyStrategy;
  private final InjectionTargetExtensionStore injectionTargetExtensionStore;

  /**
   * Constructs a new instance.
   *
   * @param resolver a {@link Resolver}, cannot be {@code null}
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   * @param injectionTargetExtensionStore an {@link InjectionTargetExtensionStore}, cannot be {@code null}
   */
  public InstantiationContextFactory(Resolver<Injectable<?>> resolver, AnnotationStrategy annotationStrategy, ProxyStrategy proxyStrategy, InjectionTargetExtensionStore injectionTargetExtensionStore) {
    this.resolver = Objects.requireNonNull(resolver, "resolver");
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.proxyStrategy = Objects.requireNonNull(proxyStrategy, "proxyStrategy");
    this.injectionTargetExtensionStore = Objects.requireNonNull(injectionTargetExtensionStore, "injectionTargetExtensionStore");
  }

  public <T> InstantiationContext<T> createContext(Binding binding) {
    return binding.associateIfAbsent("instantiationContext", () -> new SimpleInstantationContext<>(new Key(binding.getType(), binding.getQualifiers()), binding.isOptional()));
  }

  /**
   * Returns an {@link InstantiationContext} for the given {@link Key} and whether it
   * is optional or not. If an {@link InstantiationContext} is optional, it is allowed
   * to return {@code null} when no instances could be created, otherwise it will throw
   * an {@link UnsatisfiedResolutionException}.
   *
   * @param <T> the type of instances the context creates
   * @param key a {@link Key}, cannot be {@code null}
   * @param optional {@code true} if optional, otherwise {@code false}
   * @return an {@link InstantiationContext}, never {@code null}
   */
  public <T> InstantiationContext<T> createContext(Key key, boolean optional) {
    return new SimpleInstantationContext<>(key, optional);
  }

  class SimpleInstantationContext<T> implements InstantiationContext<T> {
    private final InjectionTargetExtension<T, Object> injectionTargetExtension;
    private final InstantiationContext<Object> subcontext;
    private final Key key;
    private final boolean optional;

    SimpleInstantationContext(Key key, boolean optional) {
      this.key = key;
      this.optional = optional;

      Type type = key.getType();

      this.injectionTargetExtension = injectionTargetExtensionStore.getExtension(Types.raw(type));

      Type elementType = injectionTargetExtension == null ? null : injectionTargetExtension.getElementType(type);

      this.subcontext = elementType == null ? null : createContext(new Key(elementType, key.getQualifiers()), optional);
    }

    @Override
    public synchronized T create() throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException {
      if(subcontext == null) {
        @SuppressWarnings("unchecked")
        Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

        if(injectables.size() > 1) {
          throw new AmbiguousResolutionException("Multiple matching instances: [" + key + "]: " + injectables);
        }

        T instance = injectables.size() == 0 ? null : createInstance(injectables.iterator().next());

        if(instance == null && !optional) {
          throw new UnsatisfiedResolutionException("No such instance: [" + key + "]");
        }

        return instance;
      }

      return injectionTargetExtension.getInstance(subcontext);
    }

    @Override
    public synchronized List<T> createAll() throws CreationException {
      if(subcontext == null) {
        List<T> instances = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

        for(Injectable<T> injectable : injectables) {
          T instance = createInstanceInScope(injectable);

          if(instance != null) {
            instances.add(instance);
          }
        }

        if(instances.isEmpty() && optional) {
          return null;
        }

        return instances;
      }

      /*
       * Obtaining all instances of an extended type is not supported; we could throw an
       * exception but that sort of breaks the contract of this method (and has its effect
       * when calling Injector#getInstances with an extended type like Provider<String>).
       *
       * Better is to return the empty list in those cases as getInstances does not normally
       * fail with an UnstatisfiedDependencyException. This is also more in line with #create
       * which returns null when nothing is found.
       *
       * It is hard to find all instances of an extended type as these are not part of the
       * store. Basically we'd have to find all the matching injectables, and then wrap them
       * with the extended type, which does not seem worth it. Better to force the user to
       * obtain their results differently.
       */

      return List.of();
    }

    @Override
    public InstantiationContext<T> select(Annotation... qualifiers) {
      return select(key.getType(), qualifiers);
    }

    @Override
    public <U extends T> InstantiationContext<U> select(Class<U> subtype, Annotation... qualifiers) {
      return select(subtype, qualifiers);
    }

    @Override
    public <U extends T> InstantiationContext<U> select(Type subtype, Annotation... qualifiers) {
      if(!TypeUtils.isAssignable(subtype, key.getType())) {
        throw new IllegalArgumentException("subtype must be a subtype of: " + key.getType());
      }

      Arrays.stream(qualifiers).filter(annotation -> !annotationStrategy.isQualifier(annotation)).findFirst().ifPresent(annotation -> {
        throw new IllegalArgumentException(annotation + " is not a qualifier annotation");
      });

      Key subkey = new Key(subtype, Stream.concat(key.getQualifiers().stream(), Arrays.stream(qualifiers)).collect(Collectors.toSet()));

      return createContext(subkey, optional);
    }

    private T createInstanceInScope(Injectable<T> injectable) throws CreationException {
      try {
        return injectable.getScopeResolver().isActive() ? createInstance(injectable) : null;
      }
      catch(ScopeNotActiveException e) {

        /*
         * Scope was checked to be active (to avoid exception cost), but it still occurred...
         */

        LOGGER.warning("Scope " + injectable.getScopeResolver().getAnnotation() + " should have been active: " + e.getMessage());

        return null;  // same as if scope hadn't been active in the first place
      }
    }

    private T createInstance(Injectable<T> injectable) throws CreationException, ScopeNotActiveException {
      ExtendedScopeResolver scopeResolver = injectable.getScopeResolver();
      LazyCreationalContext<?> parentCreationalContext = stack.isEmpty() ? null : stack.getLast();
      LazyCreationalContext<T> creationalContext = new LazyCreationalContext<>(parentCreationalContext, injectable);

      stack.addLast(creationalContext);

      try {
        Annotation parentScopeAnnotation = parentCreationalContext == null ? null : parentCreationalContext.injectable.getScopeResolver().getAnnotation();
        boolean needsProxy = !scopeResolver.isPseudoScope() && parentScopeAnnotation != null && !scopeResolver.getAnnotation().equals(parentScopeAnnotation);

        T instance = needsProxy
          ? proxyStrategy.<T>createProxyFactory(Types.raw(injectable.getType())).apply(() -> scopeResolver.get(injectable, creationalContext))
          : scopeResolver.get(injectable, creationalContext);

        creationalContext.add(injectable, instance);

        return instance;
      }
      catch(ScopeNotActiveException | CreationException e) {
        throw e;
      }
      catch(Exception e) {  // as extensions are called, a general catch all is used here to wrap unexpected exceptions with some useful diagnostics
        throw new CreationException("[" + injectable.getType() + "] could not be created", e);
      }
      finally {
        stack.removeLast();
      }
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
    public Reference<T> create() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
      return new LazyReference<>(this, injectable.create(getInjections()));
    }

    private List<Injection> getInjections() throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
      try {
        if(injections == null) {
          List<Injection> injections = new ArrayList<>();

          for(Binding binding : injectable.getBindings()) {
            injections.add(new Injection(binding.getAccessibleObject(), createContext(binding).create()));
          }

          this.injections = injections;
        }

        return injections;
      }
      catch(ScopeException e) {
        throw new AssertionError("Unexpected scope problem", e);  // should not occur as consistency checks during registration enforce the use of a provider or proxy
      }
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

  private class LazyReference<T> implements CreationalContext.Reference<T> {
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
