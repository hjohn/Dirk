package org.int4.dirk.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.int4.dirk.api.instantiation.AmbiguousResolutionException;
import org.int4.dirk.api.instantiation.UnsatisfiedResolutionException;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Types;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

/**
 * An {@link InjectionTargetExtension} which provides a partial implementation of the {@link Instance}
 * provider type.
 *
 * @param <T> the provided type
 */
public class InstanceInjectionTargetExtension<T> implements InjectionTargetExtension<Instance<T>, T> {
  private static final TypeVariable<?> TYPE_VARIABLE = Instance.class.getTypeParameters()[0];
  private static final Set<TypeTrait> LAZY = Collections.unmodifiableSet(EnumSet.of(TypeTrait.LAZY));

  @Override
  public Class<?> getTargetClass() {
    return Instance.class;
  }

  @Override
  public Type getElementType(Type type) {
    return Types.getTypeParameter(type, Instance.class, TYPE_VARIABLE);
  }

  @Override
  public Set<TypeTrait> getTypeTraits() {
    return LAZY;
  }

  @Override
  public Instance<T> getInstance(InstantiationContext<T> context) {
    return new DefaultInstance<>(context);
  }

  private static final class DefaultInstance<T> implements Instance<T> {
    final InstantiationContext<T> context;

    DefaultInstance(InstantiationContext<T> context) {
      this.context = context;
    }

    @Override
    public T get() {
      return context.create();
    }

    @Override
    public Iterator<T> iterator() {
      return context.createAll().iterator();
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
        context.create();

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
        context.create();

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
        context.create();

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
