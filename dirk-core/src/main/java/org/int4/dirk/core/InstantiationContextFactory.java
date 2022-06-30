package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.RootInstantiationContextFactory.RootInstantiationContext;
import org.int4.dirk.core.definition.Binding;
import org.int4.dirk.core.definition.ExtendedScopeResolver;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.definition.InjectionTarget;
import org.int4.dirk.core.definition.InjectionTargetExtensionStore;
import org.int4.dirk.core.definition.Instantiator;
import org.int4.dirk.core.definition.injection.Injection;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.core.util.Resolver;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstanceProvider;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.spi.scope.CreationalContext;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.util.Types;

/**
 * Factory for {@link InstantiationContext}s.
 */
class InstantiationContextFactory {
  private static final Logger LOGGER = Logger.getLogger(InstantiationContextFactory.class.getName());
  private static final ExtendedCreationalContext<?> NULL_CONTEXT = new FixedCreationalContext<>(null);

  private static boolean strictOrder;

  private final ProxyStrategy proxyStrategy;
  private final InjectionTargetExtensionStore injectionTargetExtensionStore;
  private final RootInstantiationContextFactory rootInstantiationContextFactory;
  private final ThreadLocal<Deque<ExtendedCreationalContext<?>>> stack = ThreadLocal.withInitial(ArrayDeque::new);

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   * @param injectionTargetExtensionStore an {@link InjectionTargetExtensionStore}, cannot be {@code null}
   */
  InstantiationContextFactory(AnnotationStrategy annotationStrategy, ProxyStrategy proxyStrategy, InjectionTargetExtensionStore injectionTargetExtensionStore) {
    this.proxyStrategy = Objects.requireNonNull(proxyStrategy, "proxyStrategy");
    this.injectionTargetExtensionStore = Objects.requireNonNull(injectionTargetExtensionStore, "injectionTargetExtensionStore");
    this.rootInstantiationContextFactory = new RootInstantiationContextFactory(annotationStrategy);
  }

  <T> InstantiationContext<T> createContext(Resolver<Injectable<?>> resolver, Key key, boolean optional) {
    return rootInstantiationContextFactory.create(resolver, createInstantiatorInternal(key, optional, null));
  }

  <T> Instantiator<T> createInstantiator(Key key, boolean optional, Annotation parentScope) {
    return createInstantiatorInternal(key, optional, parentScope);
  }

  private <T, E> DefaultInstantiator<T, E> createInstantiatorInternal(Key key, boolean optional, Annotation parentScope) {
    InjectionTargetExtension<T, E> injectionTargetExtension = injectionTargetExtensionStore.getExtension(key.getType());

    return new DefaultInstantiator<>(
      key,
      optional,
      injectionTargetExtension == null ? null : new Key(Types.getTypeParameter(key.getType(), injectionTargetExtension.getElementTypeVariable()), key.getQualifiers()),
      injectionTargetExtension == null ? null : injectionTargetExtension.getInstanceProvider(),
      injectionTargetExtension == null ? Resolution.EAGER_ONE : injectionTargetExtension.getResolution(),
      parentScope
    );
  }

  /**
   * For test purposes, sorts the resolved beans before using them.
   */
  static void useStrictOrdering() {
    strictOrder = true;
  }

  final class DefaultInstantiator<T, E> implements Instantiator<T> {
    private final Key key;
    private final boolean optional;
    private final Annotation parentScope;
    private final DefaultInstantiator<E, ?> elementInstantiator;
    private final InstanceProvider<T, E> instanceProvider;
    private final Resolution resolution;
    private final Key elementKey;

    DefaultInstantiator(Key key, boolean optional, Key elementKey, InstanceProvider<T, E> instanceProvider, Resolution resolution, Annotation parentScope) {
      this.key = key;
      this.optional = optional;
      this.elementKey = elementKey;
      this.instanceProvider = instanceProvider;
      this.resolution = resolution;
      this.parentScope = parentScope;
      this.elementInstantiator = instanceProvider == null ? null : createInstantiatorInternal(elementKey, optional, resolution == Resolution.LAZY ? null : parentScope);
    }

    @Override
    public Resolution getResolution() {
      return resolution;
    }

    @Override
    public Key getElementKey() {
      return elementKey == null ? key : elementKey;
    }

    Key getKey() {
      return key;
    }

    boolean isExtended() {
      return elementInstantiator != null;
    }

    <U extends T> DefaultInstantiator<U, ?> deriveSubInstantiator(Key key) {
      return createInstantiatorInternal(key, optional, parentScope);
    }

    @Override
    public ExtendedCreationalContext<T> create(Resolver<Injectable<?>> resolver) throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException {
      if(elementInstantiator == null) {
        @SuppressWarnings("unchecked")
        Set<Injectable<T>> injectables = (Set<Injectable<T>>)(Set<?>)resolver.resolve(key);

        if(injectables.size() > 1) {
          throw new AmbiguousResolutionException("Multiple matching instances: [" + key + "]: " + injectables);
        }

        ExtendedCreationalContext<T> creationalContext = injectables.size() == 0 ? null : createContext(resolver, injectables.iterator().next());

        // TODO This should probably throw an IllegalProductException in the second null case
        if((creationalContext == null || creationalContext.get() == null) && !optional) {
          throw new UnsatisfiedResolutionException("No such instance: [" + key + "]");
        }

        @SuppressWarnings("unchecked")
        ExtendedCreationalContext<T> castContext = creationalContext == null ? (ExtendedCreationalContext<T>)NULL_CONTEXT : creationalContext;

        return castContext;
      }

      RootInstantiationContext<E, ?> instantiationContext = rootInstantiationContextFactory.create(resolver, elementInstantiator);
      InjectionTargetExtensionCreationalContext<T, E> creationalContext = new InjectionTargetExtensionCreationalContext<>(stack.get().isEmpty() ? null : stack.get().getLast(), resolution == Resolution.LAZY ? instantiationContext : null);

      open(resolution == Resolution.LAZY ? NULL_CONTEXT : creationalContext);

      try {
        creationalContext.initialize(instanceProvider.getInstance(instantiationContext), resolution == Resolution.LAZY);

        return creationalContext;
      }
      finally {
        close();
      }
    }

    void open(ExtendedCreationalContext<?> creationalContext) {
      stack.get().addLast(creationalContext);
    }

    void close() {
      if(stack.get().size() == 1) {
        stack.remove();
      }
      else {
        stack.get().removeLast();
      }
    }

    List<ExtendedCreationalContext<T>> createAll(Resolver<Injectable<?>> resolver) throws CreationException {
      if(elementInstantiator == null) {
        List<ExtendedCreationalContext<T>> creationalContexts = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Collection<Injectable<T>> injectables = (Collection<Injectable<T>>)(Set<?>)resolver.resolve(key);

        if(strictOrder) {
          injectables = injectables.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
        }

        for(Injectable<T> injectable : injectables) {
          ExtendedCreationalContext<T> creationalContext = createContextInScope(resolver, injectable);

          if(creationalContext != null) {
            creationalContexts.add(creationalContext);
          }
        }

        return creationalContexts.isEmpty() && optional ? null : creationalContexts;
      }

      /*
       * Obtaining all contexts of an extended type is not supported; we could throw an
       * exception but that sort of breaks the contract of this method (and has its effect
       * when calling Injector#getInstances with an extended type like Provider<String>).
       *
       * Better is to return the empty list in those cases as getInstances does not normally
       * fail with an UnstatisfiedDependencyException. This is also more in line with #create
       * which returns null when nothing is found.
       *
       * It is hard to find all contexts of an extended type as these are not part of the
       * store. Basically we'd have to find all the matching injectables, and then wrap them
       * with the extended type, which does not seem worth it. Better to force the user to
       * obtain their results differently.
       */

      return List.of();
    }

    private ExtendedCreationalContext<T> createContextInScope(Resolver<Injectable<?>> resolver, Injectable<T> injectable) throws CreationException {
      try {
        if(!injectable.getScopeResolver().isDependentScope() && !injectable.getScopeResolver().isActive()) {
          return null;
        }

        return createContext(resolver, injectable);
      }
      catch(ScopeNotActiveException e) {

        /*
         * Scope was checked to be active (to avoid exception cost), but it still occurred...
         */

        LOGGER.warning("Scope " + injectable.getScopeResolver().getAnnotation() + " should have been active: " + e.getMessage());

        return null;  // same as if scope hadn't been active in the first place
      }
    }

    private ExtendedCreationalContext<T> createContext(Resolver<Injectable<?>> resolver, Injectable<T> injectable) throws CreationException, ScopeNotActiveException {
      try {
        ExtendedScopeResolver scopeResolver = injectable.getScopeResolver();
        boolean needsProxy = parentScope != null && !scopeResolver.isPseudoScope() && !scopeResolver.getAnnotation().equals(parentScope);

        if(needsProxy) {
          try {
            T instance = proxyStrategy.<T>createProxyFactory(Types.raw(injectable.getType())).apply(() -> createContext(resolver, scopeResolver, injectable).get());

            return new FixedCreationalContext<>(instance);
          }
          catch(Exception e) {  // as extensions are called, a general catch all is used here to wrap unexpected exceptions with some useful diagnostics
            throw new CreationException("[" + injectable.getType() + "] proxy could not be created", e);
          }
        }

        return createContext(resolver, scopeResolver, injectable);
      }
      catch(ScopeNotActiveException | CreationException e) {  // Avoid wrapping these exceptions in another layer
        throw e;
      }
      catch(Exception e) {  // as extensions are called, a general catch all is used here to wrap unexpected exceptions with some useful diagnostics
        throw new CreationException("[" + injectable.getType() + "] could not be created", e);
      }
    }

    private ExtendedCreationalContext<T> createContext(Resolver<Injectable<?>> resolver, ExtendedScopeResolver scopeResolver, Injectable<T> injectable) throws ScopeNotActiveException, Exception {
      @SuppressWarnings("unchecked")
      ExtendedCreationalContext<T> existingCreationalContext = (ExtendedCreationalContext<T>)scopeResolver.find(injectable);

      if(existingCreationalContext != null) {
        return existingCreationalContext;
      }

      LazyCreationalContext<T> creationalContext = new LazyCreationalContext<>(stack.get().isEmpty() ? null : stack.get().getLast(), injectable);

      open(creationalContext);

      try {
        creationalContext.initialize(createInstance(resolver, injectable));

        scopeResolver.put(injectable, creationalContext);

        return creationalContext;
      }
      finally {
        close();
      }
    }

    void destroy(CreationalContext<T> creationalContext) {
      creationalContext.release();
    }

    private T createInstance(Resolver<Injectable<?>> resolver, Injectable<T> injectable) throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
      try {
        List<Injection> injections = new ArrayList<>();

        for(InjectionTarget injectionTarget : injectable.getInjectionTargets()) {
          Binding binding = injectionTarget.getBinding();

          injections.add(new Injection(binding.getAccessibleObject(), injectionTarget.getInstantiator().create(resolver).get()));
        }

        return injectable.create(injections);
      }
      catch(ScopeException e) {
        throw new AssertionError("Unexpected scope problem", e);  // should not occur as consistency checks during registration enforce the use of a provider or proxy
      }
    }
  }

  interface ExtendedCreationalContext<T> extends CreationalContext<T> {
    void attach(ExtendedCreationalContext<?> creationalContext);
    boolean needsDestroy();
  }

  private static final class FixedCreationalContext<T> implements ExtendedCreationalContext<T> {
    private final T instance;

    FixedCreationalContext(T instance) {
      this.instance = instance;
    }

    @Override
    public T get() {
      return instance;
    }

    @Override
    public void release() {
    }

    @Override
    public boolean needsDestroy() {
      return false;
    }

    @Override
    public void attach(ExtendedCreationalContext<?> creationalContext) {
      throw new IllegalStateException("Incorrectly implemented extension. Lazy extensions are not allowed to access the creational context during instance creation!");
    }
  }

  private static final class InjectionTargetExtensionCreationalContext<T, E> implements ExtendedCreationalContext<T> {
    private final ExtendedCreationalContext<?> parent;
    private final RootInstantiationContext<E, ?> instantiationContext;

    private T instance;
    private boolean needsDestroy;
    private boolean initialized;
    private List<CreationalContext<?>> children;

    InjectionTargetExtensionCreationalContext(ExtendedCreationalContext<?> parent, RootInstantiationContext<E, ?> context) {
      this.parent = parent;
      this.instantiationContext = context;
    }

    void initialize(T instance, boolean needsDestroy) {
      this.instance = instance;
      this.needsDestroy = needsDestroy || children != null;  // if children is not null, it is not empty

      initialized = true;

      if(parent != null) {
        parent.attach(this);
      }
    }

    @Override
    public T get() {
      if(!initialized) {
        throw new IllegalStateException();
      }

      return instance;
    }

    @Override
    public final void attach(ExtendedCreationalContext<?> child) {
      if(child.needsDestroy()) {
        if(children == null) {
          children = new ArrayList<>();
        }

        children.add(child);
      }
    }

    @Override
    public void release() {
      if(instantiationContext != null) {
        instantiationContext.release();
      }

      if(children != null) {
        for(CreationalContext<?> child : children) {
          child.release();
        }
      }
    }

    @Override
    public boolean needsDestroy() {
      return needsDestroy;
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
   * call recursively into a {@link InstantiationContext}.
   */
  private class LazyCreationalContext<T> implements ExtendedCreationalContext<T> {
    private final ExtendedCreationalContext<?> parent;
    private final Injectable<T> injectable;

    private T instance;
    private boolean initialized;
    private boolean released;
    private List<CreationalContext<?>> children;

    LazyCreationalContext(ExtendedCreationalContext<?> parent, Injectable<T> injectable) {
      this.parent = parent;
      this.injectable = injectable;
    }

    void initialize(T instance) {
      this.instance = instance;
      this.initialized = true;

      if(parent != null) {
        parent.attach(this);
      }
    }

    @Override
    public void attach(ExtendedCreationalContext<?> creationalContext) {
      if(creationalContext.needsDestroy()) {
        if(children == null) {
          children = new ArrayList<>();
        }

        children.add(creationalContext);
      }
    }

    @Override
    public boolean needsDestroy() {
      if(!initialized) {
        throw new IllegalStateException("called too early");
      }

      return injectable.getScopeResolver().isDependentScope() && (injectable.needsDestroy() || children != null);  // if children is not null, it is not empty
    }

    @Override
    public synchronized T get() {
      if(released) {
        throw new IllegalStateException("context was already released");
      }

      if(!initialized) {
        throw new IllegalStateException("context was not initialized");
      }

      return instance;
    }

    @Override
    public synchronized void release() {
      if(!released) {
        injectable.destroy(instance);

        released = true;
        instance = null;

        if(children != null) {
          for(CreationalContext<?> child : children) {
            child.release();
          }
        }
      }
    }
  }
}
