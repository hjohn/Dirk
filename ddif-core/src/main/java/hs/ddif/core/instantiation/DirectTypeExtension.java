package hs.ddif.core.instantiation;

import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.instantiation.domain.MultipleInstances;
import hs.ddif.core.instantiation.domain.NoSuchInstance;
import hs.ddif.core.store.Key;

import java.lang.reflect.AnnotatedElement;
import java.util.Set;

/**
 * Type extension for {@link Instantiator}s which attempt to directly create or
 * implement types. This is the most basic form of instantiation of a type that
 * is supported by default.
 *
 * @param <T> the instantiated type
 */
public class DirectTypeExtension<T> implements TypeExtension<T> {

  @Override
  public Instantiator<T> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    boolean optional = TypeExtension.isOptional(element);

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return key;
      }

      @Override
      public T getInstance(InstantiationContext context) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance {
        Set<Injectable> injectables = context.resolve(key);

        if(injectables.size() > 1) {
          throw new MultipleInstances(key, injectables);
        }

        if(injectables.size() == 0) {
          if(optional) {
            return null;
          }

          throw new NoSuchInstance(key);
        }

        return context.createInstance(injectables.iterator().next());
      }

      @Override
      public boolean requiresAtLeastOne() {
        return !optional;
      }

      @Override
      public boolean requiresAtMostOne() {
        return true;
      }
    };
  }
}
