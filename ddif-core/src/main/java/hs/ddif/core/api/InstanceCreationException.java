package hs.ddif.core.api;

/**
 * Thrown when creation of a new instance fails while attempting to provide
 * a matching instance.
 */
public class InstanceCreationException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public InstanceCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
