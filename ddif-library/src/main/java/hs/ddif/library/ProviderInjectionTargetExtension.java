package hs.ddif.library;

import hs.ddif.spi.instantiation.InjectionTarget;
import hs.ddif.spi.instantiation.InstantiationContext;
import hs.ddif.spi.instantiation.Instantiator;
import hs.ddif.spi.instantiation.InstantiatorFactory;
import hs.ddif.spi.instantiation.Key;
import hs.ddif.spi.instantiation.InjectionTargetExtension;
import hs.ddif.spi.instantiation.TypeTrait;
import hs.ddif.util.Types;

import java.lang.reflect.TypeVariable;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configurable provider {@link InjectionTargetExtension} which allows selecting the type of provider
 * it should handle.
 *
 * @param <P> the type of the provider
 * @param <T> the type the provider provides
 */
public class ProviderInjectionTargetExtension<P, T> implements InjectionTargetExtension<P> {
  private final Class<P> providerClass;
  private final Function<Supplier<T>, P> providerFactory;
  private final TypeVariable<?> typeVariable;

  /**
   * Constructs a new instance.
   *
   * @param providerClass a {@link Class} representing the provider type, cannot be {@code null}
   * @param providerFactory a function to create the provider instance given a supplier, cannot be {@code null}
   */
  public ProviderInjectionTargetExtension(Class<P> providerClass, Function<Supplier<T>, P> providerFactory) {
    this.providerClass = Objects.requireNonNull(providerClass, "providerClass cannot be null");
    this.providerFactory = Objects.requireNonNull(providerFactory, "providerFactory cannot be null");
    this.typeVariable = providerClass.getTypeParameters()[0];
  }

  @Override
  public Class<?> getInstantiatorType() {
    return providerClass;
  }

  @Override
  public Instantiator<P> create(InstantiatorFactory instantiatorFactory, InjectionTarget injectionTarget) {
    Key key = injectionTarget.getKey();
    Key elementKey = new Key(Types.getTypeParameter(key.getType(), providerClass, typeVariable), key.getQualifiers());
    Instantiator<T> instantiator = instantiatorFactory.getInstantiator(new InjectionTarget() {
      @Override
      public Key getKey() {
        return elementKey;
      }

      @Override
      public boolean isOptional() {
        return injectionTarget.isOptional();
      }
    });
    Set<TypeTrait> typeTraits = Stream.concat(instantiator.getTypeTraits().stream(), Stream.of(TypeTrait.LAZY)).collect(Collectors.toUnmodifiableSet());

    return new Instantiator<>() {
      @Override
      public Key getKey() {
        return instantiator.getKey();
      }

      @Override
      public P getInstance(InstantiationContext context) {
        return providerFactory.apply(() -> instantiator.getInstance(context));
      }

      @Override
      public Set<TypeTrait> getTypeTraits() {
        return typeTraits;
      }
    };
  }
}