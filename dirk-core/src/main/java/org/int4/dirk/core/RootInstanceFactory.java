package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.int4.dirk.api.TypeLiteral;
import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.core.InstanceFactory.DefaultInstantiator;
import org.int4.dirk.core.InstanceFactory.ExtendedCreationalContext;
import org.int4.dirk.core.definition.Injectable;
import org.int4.dirk.core.util.Key;
import org.int4.dirk.core.util.Resolver;
import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.instantiation.Instance;
import org.int4.dirk.spi.scope.CreationalContext;

class RootInstanceFactory {
  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   */
  RootInstanceFactory(AnnotationStrategy annotationStrategy) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
  }

  /**
   * Returns an {@link RootInstance} for the given {@link Key} and whether it
   * is optional or not. If an {@link RootInstance} is optional, it is allowed
   * to return {@code null} when no instances could be created, otherwise it will throw
   * an {@link UnsatisfiedResolutionException}.
   *
   * @param <T> the type of instances the context creates
   * @param <E> the element type of instances the context creates
   * @param instantiator a {@link DefaultInstantiator}, cannot be {@code null}
   * @return a {@link RootInstance}, never {@code null}
   */
  <T, E> RootInstance<T, E> create(Resolver<Injectable<?>> resolver, DefaultInstantiator<T, E> instantiator) {
    return new RootInstance<>(resolver, instantiator);
  }

  abstract class AbstractRootInstance<T, E> implements Instance<T> {
    protected final Resolver<Injectable<?>> resolver;
    protected final DefaultInstantiator<T, E> instantiator;

    protected AbstractRootInstance(Resolver<Injectable<?>> resolver, DefaultInstantiator<T, E> instantiator) {
      this.resolver = resolver;
      this.instantiator = instantiator;
    }

    @Override
    public final T get() throws CreationException, UnsatisfiedResolutionException, AmbiguousResolutionException, ScopeNotActiveException {
      ExtendedCreationalContext<T> creationalContext = instantiator.create(resolver);

      if(creationalContext.needsDestroy()) {
        storeCreationalContext(creationalContext);
      }

      return creationalContext.get();
    }

    @Override
    public final List<T> getAll() throws CreationException {
      List<ExtendedCreationalContext<T>> creationalContexts = instantiator.createAll(resolver);

      if(creationalContexts == null) {
        return null;
      }

      List<T> instances = new ArrayList<>();

      for(ExtendedCreationalContext<T> creationalContext : creationalContexts) {
        T instance = creationalContext.get();

        if(instance != null) {
          instances.add(instance);

          if(creationalContext.needsDestroy()) {
            storeCreationalContext(creationalContext);
          }
        }
      }

      return instances;
    }

    @Override
    public final Instance<T> select(Annotation... qualifiers) {
      return createChildContext(new Key(instantiator.getKey().getType(), mergeQualifiers(instantiator.getKey(), qualifiers)));
    }

    @Override
    public final <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
      return createChildContext(new Key(subtype, mergeQualifiers(instantiator.getKey(), qualifiers)));
    }

    @Override
    public final <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
      return createChildContext(new Key(subtype.getType(), mergeQualifiers(instantiator.getKey(), qualifiers)));
    }

    private Set<Annotation> mergeQualifiers(Key key, Annotation... qualifiers) {
      Arrays.stream(qualifiers).filter(annotation -> !annotationStrategy.isQualifier(annotation)).findFirst().ifPresent(annotation -> {
        throw new IllegalArgumentException(annotation + " is not a qualifier annotation");
      });

      return Stream.concat(key.getQualifiers().stream(), Arrays.stream(qualifiers)).collect(Collectors.toSet());
    }

    protected abstract <U extends T> Instance<U> createChildContext(Key key);
    protected abstract void storeCreationalContext(CreationalContext<T> creationalContext);
  }

  /**
   * The root {@link Instance} is the context type that is injected or returned
   * when specifically requested. The type it represents can be further refined to sub types,
   * but not be made more general. When the type is refined, a child {@link Instance}
   * is returned which delegates to a root context.
   *
   * @param <T> the type the context can create
   * @param <E> the element type (if any) if the type created is an extended type
   */
  class RootInstance<T, E> extends AbstractRootInstance<T, E> {
    private final Map<Identity<T>, CreationalContext<T>> creationalContexts = new LinkedHashMap<>();

    RootInstance(Resolver<Injectable<?>> resolver, DefaultInstantiator<T, E> instantiator) {
      super(resolver, instantiator);
    }

    @Override
    protected <U extends T> Instance<U> createChildContext(Key key) {
      @SuppressWarnings("unchecked")  // safe cast as parent will accept sub types of T
      RootInstance<U, E> castParent = (RootInstance<U, E>)this;
      @SuppressWarnings("unchecked")
      DefaultInstantiator<U, E> subInstantiator = (DefaultInstantiator<U, E>)instantiator.deriveSubInstantiator(key);

      return new ChildInstance<>(resolver, subInstantiator, castParent);
    }

    synchronized boolean hasContextFor(T instance) {
      return creationalContexts.containsKey(new Identity<>(instance));
    }

    @Override
    public synchronized void destroy(T instance) {
      CreationalContext<T> creationalContext = creationalContexts.remove(new Identity<>(instance));

      if(creationalContext != null) {
        instantiator.destroy(creationalContext);  // it will only be dependent here as nothing else is stored in the instance map
      }
    }

    @Override
    public synchronized void destroyAll(Collection<T> instances) {
      if(instantiator.isExtended()) {
        throw new IllegalStateException("Can only destroy multiple instances of unextended types");
      }

      for(T instance : instances) {
        destroy(instance);
      }
    }

    @Override
    protected synchronized void storeCreationalContext(CreationalContext<T> creationalContext) {
      creationalContexts.put(new Identity<>(creationalContext.get()), creationalContext);
    }

    synchronized void release() {
      for(CreationalContext<T> creationalContext : creationalContexts.values()) {
        instantiator.destroy(creationalContext);
      }

      creationalContexts.clear();
    }
  }

  /**
   * A child {@link Instance} exists to specialize the type provide by a
   * root context. It delegates most of its functions to the root context. Specifically,
   * the tracking of dependents is handled by the root instance.
   *
   * @param <T> the type the context can create
   * @param <E> the element type (if any) if the type created is an extended type
   */
  class ChildInstance<T, E> extends AbstractRootInstance<T, E> {
    private final RootInstance<T, E> parent;

    private ChildInstance(Resolver<Injectable<?>> resolver, DefaultInstantiator<T, E> instantiator, RootInstance<T, E> parent) {
      super(resolver, instantiator);

      this.parent = parent;
    }

    @Override
    protected <U extends T> Instance<U> createChildContext(Key key) {
      @SuppressWarnings("unchecked")  // safe cast as parent will accept sub types of T
      RootInstance<U, E> castParent = (RootInstance<U, E>)parent;
      @SuppressWarnings("unchecked")
      DefaultInstantiator<U, E> subInstantiator = (DefaultInstantiator<U, E>)instantiator.deriveSubInstantiator(key);

      return new ChildInstance<>(resolver, subInstantiator, castParent);
    }

    @Override
    public void destroy(T instance) {
      parent.destroy(instance);
    }

    @Override
    public final void destroyAll(Collection<T> instances) {
      parent.destroyAll(instances);
    }

    @Override
    protected void storeCreationalContext(CreationalContext<T> creationalContext) {
      parent.storeCreationalContext(creationalContext);
    }
  }

  private static class Identity<T> {
    final T instance;

    Identity(T instance) {
      this.instance = instance;
    }

    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof Identity)) {
        return false;
      }

      return ((Identity<?>)obj).instance == instance;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(instance);
    }
  }
}
