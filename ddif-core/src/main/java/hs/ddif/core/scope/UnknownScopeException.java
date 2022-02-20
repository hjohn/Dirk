package hs.ddif.core.scope;

/**
 * Thrown when an attempt is made to register an injectable which was annotated
 * with a {@link javax.inject.Scope} for which no corresponding {@link hs.ddif.core.scope.ScopeResolver} was provided.
 */
public class UnknownScopeException extends RuntimeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message describing the error
   */
  public UnknownScopeException(String message) {
    super(message);
  }
}
