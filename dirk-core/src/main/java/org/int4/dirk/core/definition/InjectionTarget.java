package org.int4.dirk.core.definition;

/**
 * Represents a target, like a field or parameter, that can be injected.
 */
public interface InjectionTarget {

  /**
   * Returns the {@link Binding} for this target.
   *
   * @return a {@link Binding}, never {@code null}
   */
  Binding getBinding();

  /**
   * Returns the {@link Instantiator} for this target.
   *
   * @param <T> the type instantiated
   * @return an {@link Instantiator}, never {@code null}
   */
  <T> Instantiator<T> getInstantiator();
}
