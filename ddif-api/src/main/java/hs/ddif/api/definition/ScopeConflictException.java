package hs.ddif.api.definition;

/**
 * Thrown during registration when an injectable is determined to be dependent on
 * an injectable with a narrower scope.
 */
public class ScopeConflictException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a cause
   */
  public ScopeConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
