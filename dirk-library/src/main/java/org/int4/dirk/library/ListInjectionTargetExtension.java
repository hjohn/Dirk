package org.int4.dirk.library;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.int4.dirk.api.instantiation.CreationException;
import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Types;

/**
 * An {@link InjectionTargetExtension} for {@link List}s.
 *
 * @param <T> the type of element in the collection
 */
public class ListInjectionTargetExtension<T> implements InjectionTargetExtension<List<T>, T> {
  private static final TypeVariable<?> TYPE_VARIABLE = List.class.getTypeParameters()[0];
  private static final Set<TypeTrait> NONE = EnumSet.noneOf(TypeTrait.class);

  @Override
  public Class<?> getTargetClass() {
    return List.class;
  }

  @Override
  public Type getElementType(Type type) {
    return Types.getTypeParameter(type, List.class, TYPE_VARIABLE);
  }

  @Override
  public Set<TypeTrait> getTypeTraits() {
    return NONE;
  }

  @Override
  public List<T> getInstance(InstantiationContext<T> context) throws CreationException {
    return context.createAll();
  }
}
