package hs.ddif.core.definition.injection;

import hs.ddif.api.instantiation.domain.InstanceCreationException;

import java.util.List;

/**
 * Provides an object, potentially constructing it directly or indirectly and
 * any other objects that it depends on.
 *
 * @param <T> the type of the instances produced
 */
public interface Constructable<T> {

  /**
   * Creates an instance.
   *
   * @param injections a list of {@link Injection} containing values to be injected, never {@code null} or contains {@code null}s but can be empty
   * @return an instance, or {@code null} if it could not be provided
   * @throws InstanceCreationException when instantiation fails
   */
  T create(List<Injection> injections) throws InstanceCreationException;

  /**
   * Destroys an instance.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   */
  void destroy(T instance);
}
