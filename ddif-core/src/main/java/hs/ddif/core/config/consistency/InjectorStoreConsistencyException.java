package hs.ddif.core.config.consistency;

/**
 * Base exception which signals when an Injector would no longer be able to
 * supply all dependencies for all the injectables it manages if the current action
 * would be executed.<p>
 *
 * For example, removing a class from an Injector which is required for the
 * correct functioning of another class would make the Injector unable to
 * supply that other class, and so an exception is thrown instead.
 */
public abstract class InjectorStoreConsistencyException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   */
  public InjectorStoreConsistencyException(String message) {
    super(message);
  }

}
