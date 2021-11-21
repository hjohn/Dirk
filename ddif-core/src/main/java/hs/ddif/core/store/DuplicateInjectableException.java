package hs.ddif.core.store;

/**
 * Thrown when attempting to add a type to a store which already
 * contains an {@link Injectable} matching that type.
 */
public class DuplicateInjectableException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param type a {@link Class}, cannot be null
   * @param injectable an {@link Injectable}, cannot be null
   */
  public DuplicateInjectableException(Class<?> type, Injectable injectable) {
    super(type + " already registered for: " + injectable);
  }

}
