package hs.ddif.spi.instantiation;

/**
 * Produces type specific {@link Instantiator}s.
 */
public interface InstantiatorFactory {

  /**
   * Gets an {@link Instantiator} for the given {@link InjectionTarget}.
   *
   * @param <T> the type the {@link Instantiator} produces
   * @param injectionTarget an {@link InjectionTarget}, cannot be {@code null}
   * @return an {@link Instantiator}, never {@code null}
   */
  <T> Instantiator<T> getInstantiator(InjectionTarget injectionTarget);
}
