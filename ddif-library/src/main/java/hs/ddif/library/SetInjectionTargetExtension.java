package hs.ddif.library;

import hs.ddif.api.instantiation.CreationException;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.TypeTrait;
import hs.ddif.util.Types;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link InjectionTargetExtension} for {@link Set}s.
 *
 * @param <T> the type of element in the collection
 */
public class SetInjectionTargetExtension<T> implements InjectionTargetExtension<Set<T>, T> {
  private static final TypeVariable<?> TYPE_VARIABLE = Set.class.getTypeParameters()[0];
  private static final Set<TypeTrait> NONE = EnumSet.noneOf(TypeTrait.class);

  @Override
  public Class<?> getTargetClass() {
    return Set.class;
  }

  @Override
  public Type getElementType(Type type) {
    return Types.getTypeParameter(type, Set.class, TYPE_VARIABLE);
  }

  @Override
  public Set<TypeTrait> getTypeTraits() {
    return NONE;
  }

  @Override
  public Set<T> getInstance(InstantiationContext<T> context) throws CreationException {
    List<T> instances = context.createAll();

    return instances == null ? null : new HashSet<>(instances);
  }
}
