package hs.ddif.core.inject.store;

import hs.ddif.core.definition.bind.Binding;
import hs.ddif.core.instantiation.Instantiator;

/**
 * Provides bound {@link Instantiator}s.
 */
public interface BoundInstantiatorProvider {

  /**
   * Find an {@link Instantiator} associated with the given {@link Binding}.
   *
   * @param <T> the expected type
   * @param binding a {@link Binding}, cannot be {@code null}
   * @return an {@link Instantiator} or {@code null} if the binding matched none
   */
  <T> Instantiator<T> getInstantiator(Binding binding);
}