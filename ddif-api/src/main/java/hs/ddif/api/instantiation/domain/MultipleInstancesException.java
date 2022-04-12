package hs.ddif.api.instantiation.domain;

/**
 * Thrown when multiple matching instances were available while only one
 * was expected.
 */
public class MultipleInstancesException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public MultipleInstancesException(String message, Throwable cause) {
    super(message, cause);
  }
}