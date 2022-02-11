package hs.ddif.core.instantiation.injection;

import hs.ddif.core.instantiation.domain.InstanceCreationFailure;

import java.util.List;

/**
 * Provides an object, potentially constructing it directly or indirectly and
 * any other objects that it depends on.
 */
public interface ObjectFactory {

  /**
   * Creates an instance.
   *
   * @param injections a list of {@link Injection} containing values to be injected, never null but can be empty
   * @return an instance, or <code>null</code> if it could not be provided
   * @throws InstanceCreationFailure when instantiation fails
   */
  Object createInstance(List<Injection> injections) throws InstanceCreationFailure;
}
