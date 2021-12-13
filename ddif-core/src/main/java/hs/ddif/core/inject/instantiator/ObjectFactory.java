package hs.ddif.core.inject.instantiator;

import hs.ddif.core.api.NamedParameter;

/**
 * Provides an object, potentially constructing it directly or indirectly and
 * any other objects that it depends on.
 */
public interface ObjectFactory {

  /**
   * Creates an instance.
   *
   * @param instantiator the {@link Instantiator} to use to resolve dependencies, cannot be null
   * @param parameters zero or more {@link NamedParameter} required for constructing the provided instance
   * @return an instance, or <code>null</code> if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  Object createInstance(Instantiator instantiator, NamedParameter... parameters) throws InstanceCreationFailure;
}
