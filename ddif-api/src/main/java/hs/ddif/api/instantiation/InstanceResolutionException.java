package hs.ddif.api.instantiation;

/**
 * Base class for exceptions that can be thrown during instance resolution.
 */
public abstract class InstanceResolutionException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public InstanceResolutionException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   */
  public InstanceResolutionException(String message) {
    super(message);
  }
}
