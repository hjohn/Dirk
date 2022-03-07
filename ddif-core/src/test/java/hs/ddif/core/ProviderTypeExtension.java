package hs.ddif.core;

import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.TypeTrait;
import hs.ddif.core.instantiation.domain.InstanceResolutionFailure;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Provider;

public class ProviderTypeExtension<T> implements TypeExtension<Provider<T>> {

  @Override
  public Instantiator<Provider<T>> create(InstantiatorFactory instantiatorFactory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Provider.class, Provider.class.getTypeParameters()[0]), key.getQualifiers());
    Instantiator<T> instantiator = instantiatorFactory.getInstantiator(elementKey, element);
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