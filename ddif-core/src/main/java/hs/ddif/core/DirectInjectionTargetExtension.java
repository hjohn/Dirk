package hs.ddif.core;

import hs.ddif.api.instantiation.AmbiguousResolutionException;
import hs.ddif.api.instantiation.CreationException;
import hs.ddif.api.instantiation.UnsatisfiedResolutionException;
import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.TypeTrait;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * An {@link InjectionTargetExtension} for {@link Instantiator}s which attempt to directly create or
 * implement types. This is the most basic form of instantiation of a type that
 * is supported by default.
 *
 * @param <T> the instantiated type
 */
class DirectInjectionTargetExtension<T> implements InjectionTargetExtension<T> {
  private static final Set<TypeTrait> REQUIRES_AT_MOST_ONE = Collections.unmodifiableSet(EnumSet.of(TypeTrait.REQUIRES_AT_MOST_ONE));
  private static final Set<TypeTrait> REQUIRES_EXACTLY_ONE = Collections.unmodifiableSet(EnumSet.of(TypeTrait.REQUIRES_AT_MOST_ONE, TypeTrait.REQUIRES_AT_LEAST_ONE));

  @Override
  public Class<?> getInstantiatorType() {
    throw new UnsupportedOperationException();  // not required for the default internal extension
  }

  @Override
  public Instantiator<T> create(InstantiatorFactory factory, InjectionTarget injectionTarget) {
    Set<TypeTrait> typeTraits = injectionTarget.isOptional() ? REQUIRES_AT_MOST_ONE : REQUIRES_EXACTLY_ONE;

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return injectionTarget.getKey();
      }

      @Override
      public T getInstance(InstantiationContext context) throws CreationException, AmbiguousResolutionException, UnsatisfiedResolutionException {
        T instance = context.create(injectionTarget.getKey());

        if(instance == null) {
          if(!typeTraits.contains(TypeTrait.REQUIRES_AT_LEAST_ONE)) {
            return null;
          }

          throw new UnsatisfiedResolutionException("No such instance: [" + injectionTarget.getKey() + "]");
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
