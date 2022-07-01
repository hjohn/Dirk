package org.int4.dirk.cdi;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.util.TypeVariables;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

/**
 * An {@link InjectionTargetExtension} which provides a partial implementation of the {@link Instance}
 * provider type.
 *
 * @param <T> the provided type
 */
public class InstanceInjectionTargetExtension<T> extends InjectionTargetExtension<Instance<T>, T> {

  /**
   * Constructs a new instance.
   */
  public InstanceInjectionTargetExtension() {
    super(TypeVariables.get(Instance.class, 0), Resolution.LAZY, DefaultInstance::new);
  }

  private static final class DefaultInstance<T> implements Instance<T> {
    final InstantiationContext<T> context;

    DefaultInstance(InstantiationContext<T> context) {
      this.context = context;
    }

    @Override
    public T get() {
      return context.get();
    }

    @Override
    public Iterator<T> iterator() {
      return context.getAll().iterator();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
      return new DefaultInstance<>(context.select(qualifiers));
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
      return new DefaultInstance<>(context.select(subtype, qualifiers));
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> literal, Annotation... qualifiers) {
      return new DefaultInstance<>(context.select(literal.getRawType(), qualifiers));
    }

    @Override
    public boolean isUnsatisfied() {  // TODO this implementation is sub par; improve when extensions to InstantiationContext interface are finalized
      try {
        context.get();

        return false;
      }
      catch(UnsatisfiedResolutionException e) {
        return true;
      }
      catch(Exception e) {
        return false;
      }
    }

    @Override
    public boolean isAmbiguous() {  // TODO this implementation is sub par; improve when extensions to InstantiationContext interface are finalized
      try {
        context.get();

        return false;
      }
      catch(AmbiguousResolutionException e) {
        return true;
      }
      catch(Exception e) {
        return false;
      }
    }

    @Override
    public boolean isResolvable() {  // TODO this implementation is sub par; improve when extensions to InstantiationContext interface are finalized
      try {
        context.get();

        return true;
      }
      catch(Exception e) {
        return false;
      }
    }

    @Override
    public void destroy(T instance) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Handle<T> getHandle() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
      throw new UnsupportedOperationException("Not implemented");
    }
  }
}
