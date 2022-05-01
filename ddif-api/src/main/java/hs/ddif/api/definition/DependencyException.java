package hs.ddif.api.definition;

/**
 * Base exception which signals when an Injector would no longer be able to
 * supply all dependencies for all types it manages if the current action
 * would be executed.<p>
 *
 * For example, removing a type from an Injector which is required as a
 * dependency of another type would make the Injector unable to supply that
 * other type, and so an exception is thrown instead.
 */
public abstract class DependencyException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a cause
   */
  public DependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new instance.
   *
   * @param message a message
   */
  public DependencyException(String message) {
    super(message);
  }

}
