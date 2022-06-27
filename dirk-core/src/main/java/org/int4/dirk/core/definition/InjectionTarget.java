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
}
