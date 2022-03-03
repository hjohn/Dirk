package hs.ddif.core.definition;

import hs.ddif.core.instantiation.factory.LifeCycleCallbacks;

/**
 * Factory for {@link LifeCycleCallbacks}.
 */
public interface LifeCycleCallbacksFactory {

  /**
   * Creates a {@link LifeCycleCallbacks} for a given class.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link LifeCycleCallbacks}, never {@code null}
   */
  LifeCycleCallbacks create(Class<?> cls);
}
