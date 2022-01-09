package hs.ddif.core.config.consistency;

import hs.ddif.core.scope.ScopeResolver;

import javax.inject.Scope;

/**
 * Thrown when an attempt is made to register an injectable which was annotated
 * with a {@link Scope} for which no corresponding {@link ScopeResolver} was provided.
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
