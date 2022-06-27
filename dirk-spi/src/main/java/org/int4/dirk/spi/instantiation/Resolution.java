package org.int4.dirk.spi.instantiation;

/**
 * Resolution determines how a dependency should be treated by an injector.
 * Eager resolution types are enforced at registration time, while lazy resolutions
 * are only enforced at instantiation time.
 */
public enum Resolution {

  /**
   * No restrictions are placed upon the dependency at registration time.
   *
   * <p>This type will never result in problems resolving circular dependencies or
   * scope conflicts. If requirements are not met when an instance needs to be
   * constructed with this resolution type an exception will be thrown indicating
   * the missing requirements.
   */
  LAZY,

  /**
   * Restricts the dependency to be non-circular, properly scoped (a proxy may be
   * required), and the dependency must result in a single match.
   *
   * <p>If there are no matching  dependencies then the type cannot be registered
   * unless the injection target is optional. If there are more matching dependencies
   * the type cannot be registered. If the dependency is circular or there is a scope
   * conflict that cannot be resolved, the type cannot be registered.
   */
  EAGER_ONE,

  /**
   * Restricts the dependency to be non-circular.
   *
   * <p>If the dependency is circular, the type cannot be registered.
   */
  EAGER_ANY
}