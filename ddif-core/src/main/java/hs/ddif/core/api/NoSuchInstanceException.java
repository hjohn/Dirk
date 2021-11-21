package hs.ddif.core.api;

/**
 * Thrown when no matching instance was available or could be created.
 */
public class NoSuchInstanceException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public NoSuchInstanceException(String message, Throwable cause) {
    super(message, cause);
  }
}
