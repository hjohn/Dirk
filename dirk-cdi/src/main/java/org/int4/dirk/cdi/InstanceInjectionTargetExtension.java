package org.int4.dirk.cdi;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.Instance;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.util.TypeVariables;

import jakarta.enterprise.util.TypeLiteral;

/**
 * An {@link InjectionTargetExtension} which provides a partial implementation of the {@link Instance}
 * provider type.
 *
 * @param <T> the provided type
 */
public class InstanceInjectionTargetExtension<T> extends InjectionTargetExtension<jakarta.enterprise.inject.Instance<T>, T> {

  /**
   * Constructs a new instance.
   */
  public InstanceInjectionTargetExtension() {
    super(TypeVariables.get(jakarta.enterprise.inject.Instance.class, 0), Resolution.LAZY, DefaultInstance::new);
  }

  private static final class DefaultInstance<T> implements jakarta.enterprise.inject.Instance<T> {
    final Instance<T> instance;

    DefaultInstance(Instance<T> instance) {
      this.instance = instance;
    }

    @Override
    public T get() {
      return instance.get();
    }

    @Override
    public Iterator<T> iterator() {
      return instance.getAll().iterator();
    }

    @Override
    public jakarta.enterprise.inject.Instance<T> select(Annotation... qualifiers) {
      return new DefaultInstance<>(instance.select(qualifiers));
    }

    @Override
    public <U extends T> jakarta.enterprise.inject.Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
      return new DefaultInstance<>(instance.select(subtype, qualifiers));
    }

    @Override
    public <U extends T> jakarta.enterprise.inject.Instance<U> select(TypeLiteral<U> literal, Annotation... qualifiers) {
      return new DefaultInstance<>(instance.select(literal.getRawType(), qualifiers));
    }

    @Override
    public boolean isUnsatisfied() {  // TODO this implementation is sub par; improve when extensions to Instance interface are finalized
      try {
        instance.get();

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
    public boolean isAmbiguous() {  // TODO this implementation is sub par; improve when extensions to Instance interface are finalized
      try {
        instance.get();

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
    public boolean isResolvable() {  // TODO this implementation is sub par; improve when extensions to Instance interface are finalized
      try {
        instance.get();

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
