package hs.ddif.core.config;

import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.InstanceResolutionFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.TypeVariable;

import javax.inject.Provider;

/**
 * Type extension for {@link Instantiator}s that wrap an injectable within a
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

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return instantiator.getKey();
      }

      @Override
      public Provider<T> getInstance(InstantiationContext context) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance {
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
      public boolean requiresAtLeastOne() {
        return instantiator.requiresAtLeastOne();
      }

      @Override
      public boolean requiresAtMostOne() {
        return instantiator.requiresAtMostOne();
      }

      @Override
      public boolean isLazy() {
        return true;
      }
    };
  }
}
