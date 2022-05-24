package org.int4.dirk.library;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.InstantiationContext;
import org.int4.dirk.spi.instantiation.TypeTrait;
import org.int4.dirk.util.Types;

/**
 * Configurable provider {@link InjectionTargetExtension} which allows selecting the type of provider
 * it should handle.
 *
 * @param <P> the type of the provider
 * @param <E> the type the provider provides
 */
public class ProviderInjectionTargetExtension<P, E> implements InjectionTargetExtension<P, E> {
  private static final Set<TypeTrait> LAZY = Collections.unmodifiableSet(EnumSet.of(TypeTrait.LAZY));

  private final Class<P> providerClass;
  private final Function<Supplier<E>, P> providerFactory;
  private final TypeVariable<?> typeVariable;

  /**
   * Constructs a new instance.
   *
   * @param providerClass a {@link Class} representing the provider type, cannot be {@code null}
   * @param providerFactory a function to create the provider instance given a supplier, cannot be {@code null}
   */
  public ProviderInjectionTargetExtension(Class<P> providerClass, Function<Supplier<E>, P> providerFactory) {
    this.providerClass = Objects.requireNonNull(providerClass, "providerClass cannot be null");
    this.providerFactory = Objects.requireNonNull(providerFactory, "providerFactory cannot be null");
    this.typeVariable = providerClass.getTypeParameters()[0];
  }

  @Override
  public Class<?> getTargetClass() {
    return providerClass;
  }

  @Override
  public Type getElementType(Type type) {
    return Types.getTypeParameter(type, providerClass, typeVariable);
  }

  @Override
  public Set<TypeTrait> getTypeTraits() {
    return LAZY;
  }

  @Override
  public P getInstance(InstantiationContext<E> context) {
    return providerFactory.apply(context::create);
  }
}