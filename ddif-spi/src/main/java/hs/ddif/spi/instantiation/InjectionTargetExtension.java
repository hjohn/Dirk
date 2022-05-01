package hs.ddif.spi.instantiation;

/**
 * An interface to allow for custom handling of {@link InjectionTarget}s of type
 * {@code T} using a custom {@link Instantiator}.
 *
 * <p>Whenever an {@link InjectionTarget} of type {@code T} needs injection this
 * extension will be called to provide the {@link Instantiator} which can provide
 * the value for this type of target.
 *
 * @param <T> the type handled
 */
public interface InjectionTargetExtension<T> {

  /**
   * Returns the type of the {@link Instantiator}s produced by this extension.
   *
   * @return a {@link Class}, never {@code null}
   */
  Class<?> getInstantiatorType();

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