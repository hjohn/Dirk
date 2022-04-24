package hs.ddif.api.definition;

/**
 * Base exception which signals when an Injector would no longer be able to
 * supply all dependencies for all the injectables it manages if the current action
 * would be executed.<p>
 *
 * For example, removing a class from an Injector which is required for the
 * correct functioning of another class would make the Injector unable to
 * supply that other class, and so an exception is thrown instead.
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
