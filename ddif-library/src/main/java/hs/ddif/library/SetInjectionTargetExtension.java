package hs.ddif.library;

import hs.ddif.api.instantiation.CreationException;
import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.TypeTrait;
import hs.ddif.util.Types;

import java.lang.reflect.TypeVariable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link InjectionTargetExtension} for {@link Instantiator}s that gather all matching injectables
 * in a {@link Set}.
 *
 * @param <T> the type of element in the collection
 */
public class SetInjectionTargetExtension<T> implements InjectionTargetExtension<Set<T>> {
  private static final TypeVariable<?> TYPE_VARIABLE = Set.class.getTypeParameters()[0];
  private static final Set<TypeTrait> NONE = EnumSet.noneOf(TypeTrait.class);

  @Override
  public Class<?> getInstantiatorType() {
    return Set.class;
  }

  @Override
  public Instantiator<Set<T>> create(InstantiatorFactory factory, InjectionTarget injectionTarget) {
    Key key = injectionTarget.getKey();
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), Set.class, TYPE_VARIABLE), key.getQualifiers());
    boolean optional = injectionTarget.isOptional();

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public Set<T> getInstance(InstantiationContext context) throws CreationException {
        List<T> instances = context.createAll(elementKey);

        return instances.isEmpty() && optional ? null : new HashSet<>(instances);
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return NONE;
      }
    };
  }
}
