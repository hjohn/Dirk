package hs.ddif.core.instantiation;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.Key;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Type extension for {@link Instantiator}s which attempt to directly create or
 * implement types. This is the most basic form of instantiation of a type that
 * is supported by default.
 *
 * @param <T> the instantiated type
 */
public class DirectTypeExtension<T> implements TypeExtension<T> {
  private static final Set<TypeTrait> REQUIRES_AT_MOST_ONE = Collections.unmodifiableSet(EnumSet.of(TypeTrait.REQUIRES_AT_MOST_ONE));
  private static final Set<TypeTrait> REQUIRES_EXACTLY_ONE = Collections.unmodifiableSet(EnumSet.of(TypeTrait.REQUIRES_AT_MOST_ONE, TypeTrait.REQUIRES_AT_LEAST_ONE));

  @Override
  public Instantiator<T> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Set<TypeTrait> typeTraits = TypeExtension.isOptional(element) ? REQUIRES_AT_MOST_ONE : REQUIRES_EXACTLY_ONE;

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return key;
      }

      @Override
      public T getInstance(InstantiationContext context) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance {
        T instance = context.create(key);

        if(instance == null) {
          if(!typeTraits.contains(TypeTrait.REQUIRES_AT_LEAST_ONE)) {
            return null;
          }

          throw new NoSuchInstance(key);
        }

        return instance;
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return typeTraits;
      }
    };
  }
}
