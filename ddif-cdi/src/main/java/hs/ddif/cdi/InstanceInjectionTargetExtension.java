package hs.ddif.cdi;

import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.org.apache.commons.lang3.reflect.TypeUtils;
import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.TypeTrait;
import hs.ddif.util.Annotations;
import hs.ddif.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Qualifier;

/**
 * An {@link InjectionTargetExtension} which provides a partial implementation of the {@link Instance}
 * provider type.
 *
 * @param <T> the provided type
 */
public class InstanceInjectionTargetExtension<T> implements InjectionTargetExtension<Instance<T>> {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);
  private static final TypeVariable<?> TYPE_VARIABLE = Instance.class.getTypeParameters()[0];
  private static final Set<TypeTrait> LAZY = Collections.unmodifiableSet(EnumSet.of(TypeTrait.LAZY));

  @Override
  public Class<?> getInstantiatorType() {
    return Instance.class;
  }

  @Override
  public Instantiator<Instance<T>> create(InstantiatorFactory factory, InjectionTarget injectionTarget) {
    Key key = injectionTarget.getKey();
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Instance.class, TYPE_VARIABLE), key.getQualifiers());

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public Instance<T> getInstance(InstantiationContext context) {
        return new DefaultInstance<>(context, elementKey, injectionTarget.isOptional());
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return LAZY;
      }
    };
  }

  private static final class DefaultInstance<T> implements Instance<T> {
    final InstantiationContext context;
    final Key key;
    final boolean optional;

    DefaultInstance(InstantiationContext context, Key key, boolean optional) {
      this.context = context;
      this.key = key;
      this.optional = optional;
    }

    @Override
    public T get() {
      T instance = context.create(key);

      if(instance == null) {
        if(optional) {
          return null;
        }

        throw new UnsatisfiedResolutionException("No such instance: [" + key + "]");
      }

      return instance;
    }

    @Override
    public Iterator<T> iterator() {
      return context.<T>createAll(key).iterator();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
      return selectByType(key.getType(), qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
      return selectByType(subtype, qualifiers);
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> literal, Annotation... qualifiers) {
      return selectByType(literal.getType(), qualifiers);
    }

    private <U extends T> Instance<U> selectByType(Type subtype, Annotation... qualifiers) {
      if(!TypeUtils.isAssignable(subtype, key.getType())) {
        throw new IllegalArgumentException("subtype must be a subtype of: " + key.getType());
      }

      Arrays.stream(qualifiers).filter(annotation -> !Annotations.isMetaAnnotated(annotation.annotationType(), QUALIFIER)).findFirst().ifPresent(annotation -> {
        throw new IllegalArgumentException(annotation + " is not a qualifier annotation");
      });

      Key subkey = new Key(subtype, Stream.concat(key.getQualifiers().stream(), Arrays.stream(qualifiers)).collect(Collectors.toSet()));

      return new DefaultInstance<>(context, subkey, optional);
    }

    @Override
    public boolean isUnsatisfied() {  // TODO this implementation is sub par; improve when extensions to InstantiationContext interface are finalized
      try {
        return context.create(key) == null;
      }
      catch(Exception e) {
        return false;
      }
    }

    @Override
    public boolean isAmbiguous() {  // TODO this implementation is sub par; improve when extensions to InstantiationContext interface are finalized
      try {
        context.create(key);

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
        return context.create(key) != null;
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
