package hs.ddif.core.api;

/**
 * Thrown when multiple matching instances were available while only one
 * was expected.
 */
public class MultipleInstancesException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be null
   * @param cause a {@link Throwable} cause, can be null
   */
  public MultipleInstancesException(String message, Throwable cause) {
    super(message, cause);
  }
}
