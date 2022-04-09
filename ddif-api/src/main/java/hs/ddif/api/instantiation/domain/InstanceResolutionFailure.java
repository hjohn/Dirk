package hs.ddif.api.instantiation.domain;

/**
 * Base class for exceptions that can be thrown during instance resolution.
 */
public abstract class InstanceResolutionFailure extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   * @param cause a {@link Throwable} cause, can be {@code null}
   */
  public InstanceResolutionFailure(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message, can be {@code null}
   */
  public InstanceResolutionFailure(String message) {
    super(message);
  }

  /**
   * Converts this exception to a {@link RuntimeException}.
   *
   * @return a new {@link RuntimeException}, never {@code null}
   */
  public abstract RuntimeException toRuntimeException();
}
