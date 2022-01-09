package hs.ddif.core.config.consistency;

/**
 * Thrown during registration when an injectable is determined to be depedendant on
 * an injectable with a narrower scope.
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
