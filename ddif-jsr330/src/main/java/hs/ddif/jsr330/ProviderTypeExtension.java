package hs.ddif.jsr330;

import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeTrait;
import hs.ddif.core.instantiation.domain.InstanceResolutionFailure;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Provider;

/**
 * Type extension for {@link Instantiator}s that wraps an injectable within a
 * {@link Provider} for resolution at a later time.
 *
 * @param <T> the type provided
 */
public class ProviderTypeExtension<T> implements TypeExtension<Provider<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = Provider.class.getTypeParameters()[0];

  @Override
  public Instantiator<Provider<T>> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Provider.class, TYPE_VARIABLE), key.getQualifiers());
    Instantiator<T> instantiator = factory.getInstantiator(elementKey, element);
    Set<TypeTrait> typeTraits = Collections.unmodifiableSet(Stream.concat(instantiator.getTypeTraits().stream(), Stream.of(TypeTrait.LAZY)).collect(Collectors.toSet()));

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return instantiator.getKey();
      }

      @Override
      public Provider<T> getInstance(InstantiationContext context) {
        return new Provider<>() {
          @Override
          public T get() {
            try {
              return instantiator.getInstance(context);
            }
            catch(InstanceResolutionFailure f) {
              throw f.toRuntimeException();
            }
          }
        };
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return typeTraits;
      }
    };
  }
}
