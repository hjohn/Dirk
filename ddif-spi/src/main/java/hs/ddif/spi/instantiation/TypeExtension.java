package hs.ddif.spi.instantiation;

/**
 * Interface for customizing how a specific type can be instantiated.
 *
 * @param <T> the type customized
 */
public interface TypeExtension<T> {

  /**
   * Creates a new {@link Instantiator} which will produce a type matching
   * suitable for injection into the given {@link InjectionTarget}.
   *
   * @param instantiatorFactory an {@link InstantiatorFactory} to get delegate {@link Instantiator}s, cannot be {@code null}
   * @param injectionTarget an {@link InjectionTarget}, cannot be {@code null}
   * @return an {@link Instantiator}, never {@code null}
   */
  Instantiator<T> create(InstantiatorFactory instantiatorFactory, InjectionTarget injectionTarget);

}