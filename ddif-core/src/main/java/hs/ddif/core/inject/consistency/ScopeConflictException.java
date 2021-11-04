package hs.ddif.core.inject.consistency;

/**
 * Thrown during registration when an injectable is determined to be depedendant on
 * a bean with a narrower scope.
 */
public class ScopeConflictException extends InjectorStoreConsistencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   */
  public ScopeConflictException(String message) {
    super(message);
  }
}
