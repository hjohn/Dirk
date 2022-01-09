package hs.ddif.core.inject.instantiation;

import hs.ddif.core.scope.OutOfScopeException;

/**
 * An abstraction to get values from an {@link Instantiator}.
 */
public interface ValueFactory {

  /**
   * Given an {@link Instantiator} returns a value.
   *
   * @param instantiator an {@link Instantiator} for creating further dependencies, cannot be {@code null}
   * @return a value, can be {@code null}
   * @throws OutOfScopeException when an out of scope dependency was encountered
   * @throws NoSuchInstance when no matching instance could be found or created
   * @throws MultipleInstances when multiple matching instances were found or could be created
   * @throws InstanceCreationFailure when instantiation of an instance failed
   */
  Object getValue(Instantiator instantiator) throws InstanceCreationFailure, MultipleInstances, NoSuchInstance, OutOfScopeException;
}
