package org.int4.dirk.core.definition;

import org.int4.dirk.core.util.Key;
import org.int4.dirk.spi.instantiation.Resolution;

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

  /**
   * Returns the {@link Key} of which individual elements of the injection target consist.
   * For simple types, this will be the same as the injection target's type. For types
   * which are provided by an injection target extension, this will be the base type that
   * is looked up for injection.
   *
   * @return a {@link Key}, never {@code null}
   */
  Key getElementKey();

  /**
   * Returns how the binding should be resolved.
   *
   * @return a {@link Resolution}, never {@code null}
   */
  Resolution getResolution();
}
