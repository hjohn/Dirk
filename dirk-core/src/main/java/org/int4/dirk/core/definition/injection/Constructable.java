package org.int4.dirk.core.definition.injection;

import java.util.List;

import org.int4.dirk.api.instantiation.CreationException;

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
   * @throws CreationException when instantiation fails
   */
  T create(List<Injection> injections) throws CreationException;

  /**
   * Destroys an instance.
   *
   * @param instance an instance to destroy, cannot be {@code null}
   */
  void destroy(T instance);

  /**
   * Checks whether this constructable needs to be destroyed. A constructable does
   * not need to be tracked if it doesn't need specific actions when it is about to
   * be destroyed.
   *
   * @return {@code true} if the constructable needs to be destroyed, otherwise {@code false}
   */
  boolean needsDestroy();
}
