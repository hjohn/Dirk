package hs.ddif.api.definition;

import hs.ddif.api.instantiation.LifeCycleCallbacks;

/**
 * Factory for {@link LifeCycleCallbacks}.
 */
public interface LifeCycleCallbacksFactory {

  /**
   * Creates a {@link LifeCycleCallbacks} for a given class.
   *
   * @param cls a {@link Class}, cannot be {@code null}
   * @return a {@link LifeCycleCallbacks}, never {@code null}
   * @throws DefinitionException when a definition problem was encountered
   */
  LifeCycleCallbacks create(Class<?> cls) throws DefinitionException;
}
