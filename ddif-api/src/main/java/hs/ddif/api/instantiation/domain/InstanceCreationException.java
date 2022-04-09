package hs.ddif.api.instantiation.domain;

/**
 * Thrown when creation of a new instance fails while attempting to provide
 * a matching instance.
 */
public class InstanceCreationException extends InstanceResolutionException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public InstanceCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
