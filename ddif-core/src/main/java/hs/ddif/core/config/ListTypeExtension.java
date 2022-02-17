package hs.ddif.core.config;

import hs.ddif.core.instantiation.InstantiationContext;
import hs.ddif.core.instantiation.Instantiator;
import hs.ddif.core.instantiation.InstantiatorFactory;
import hs.ddif.core.instantiation.TypeExtension;
import hs.ddif.core.instantiation.domain.InstanceCreationFailure;
import hs.ddif.core.store.Key;
import hs.ddif.core.util.Types;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.TypeVariable;
import java.util.List;

/**
 * Type extension for {@link Instantiator}s that gather all matching injectables
 * in a {@link List}.
 *
 * @param <T> the type of element in the collection
 */
public class ListTypeExtension<T> implements TypeExtension<List<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = List.class.getTypeParameters()[0];

  @Override
  public Instantiator<List<T>> create(InstantiatorFactory factory, Key key, AnnotatedElement element) {
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), List.class, TYPE_VARIABLE), key.getQualifiers());
    boolean optional = TypeExtension.isOptional(element);

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public List<T> getInstance(InstantiationContext context) throws InstanceCreationFailure {
        List<T> instances = context.createAll(elementKey);

        return instances.isEmpty() && optional ? null : instances;
      }

      @Override
      public boolean requiresAtLeastOne() {
        return false;
      }

      @Override
      public boolean requiresAtMostOne() {
        return false;
      }
    };
  }
}
