package hs.ddif.core.inject.store;

/**
 * Thrown when an attempt is made to register an injectable which was annotated
 * with a {@link javax.inject.Scope} for which no corresponding {@link hs.ddif.core.scope.ScopeResolver} was provided.
 */
public class UnknownScopeException extends InjectorStoreConsistencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message describing the error
   */
  public UnknownScopeException(String message) {
    super(message);
  }
}
