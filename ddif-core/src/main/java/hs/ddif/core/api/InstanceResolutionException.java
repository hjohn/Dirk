package hs.ddif.core.api;

/**
 * Base class for exceptions that can be thrown during instance resolution by
 * an {@link InstanceResolver}.
 */
public abstract class InstanceResolutionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public InstanceResolutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
