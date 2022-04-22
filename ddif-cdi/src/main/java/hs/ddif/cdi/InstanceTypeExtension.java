package hs.ddif.cdi;

import hs.ddif.api.instantiation.Key;
import hs.ddif.api.instantiation.NoSuchInstanceException;
import hs.ddif.api.util.Types;
import hs.ddif.org.apache.commons.lang3.reflect.TypeUtils;
import hs.ddif.spi.config.AnnotationStrategy;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.TypeExtension;
import hs.ddif.spi.instantiation.TypeTrait;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

public class InstanceTypeExtension<T> implements TypeExtension<Instance<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = Instance.class.getTypeParameters()[0];
  private static final Set<TypeTrait> LAZY = Collections.unmodifiableSet(EnumSet.of(TypeTrait.LAZY));

  private final AnnotationStrategy annotationStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   */
  public InstanceTypeExtension(AnnotationStrategy annotationStrategy) {
    this.annotationStrategy = annotationStrategy;
  }

  @Override
  public Instantiator<Instance<T>> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Instance.class, TYPE_VARIABLE), key.getQualifiers());
    boolean optional = annotationStrategy.isOptional(element);

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public Instance<T> getInstance(InstantiationContext context) {
        return new DefaultInstance<>(context, elementKey, optional);
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

        throw new NoSuchInstanceException(key);
      }

      return instance;
    }

    @Override
    public Iterator<T> iterator() {
      List<T> list = context.createAll(key);

      return list.iterator();
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

      Key subkey = new Key(subtype, Stream.concat(key.getQualifiers().stream(), Arrays.stream(qualifiers)).collect(Collectors.toSet()));  // TODO check validity of qualifiers (no duplicates, must be qualifiers)

      return new DefaultInstance<>(context, subkey, optional);
    }

    @Override
    public boolean isUnsatisfied() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean isAmbiguous() {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void destroy(T instance) {
      // TODO Auto-generated method stub

    }

    @Override
    public Handle<T> getHandle() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
