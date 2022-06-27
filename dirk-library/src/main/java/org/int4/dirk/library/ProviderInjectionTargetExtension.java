package org.int4.dirk.library;

import java.util.function.Function;
import java.util.function.Supplier;

import org.int4.dirk.spi.instantiation.InjectionTargetExtension;
import org.int4.dirk.spi.instantiation.Resolution;
import org.int4.dirk.util.TypeVariables;

/**
 * Configurable provider {@link InjectionTargetExtension} which allows selecting the type of provider
 * it should handle.
 *
 * @param <T> the type of the provider
 * @param <E> the type the provider provides
 */
public class ProviderInjectionTargetExtension<T, E> extends InjectionTargetExtension<T, E> {

  /**
   * Constructs a new instance.
   *
   * @param providerClass a {@link Class} representing the provider type, cannot be {@code null}
   * @param providerFactory a function to create the provider instance given a supplier, cannot be {@code null}
   */
  public ProviderInjectionTargetExtension(Class<T> providerClass, Function<Supplier<E>, T> providerFactory) {
    super(TypeVariables.get(providerClass, 0), Resolution.LAZY, context -> providerFactory.apply(context::create));
  }
}