package org.int4.dirk.api.instantiation;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class UnsatisfiedResolutionException extends InjectionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public UnsatisfiedResolutionException(String message) {
    super(message);
  }
}
