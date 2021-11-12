package hs.ddif.core.inject.instantiator;

import hs.ddif.core.scope.ScopeResolver;

import javax.inject.Scope;

/**
 * Thrown when an attempt is made to instantiate an injectable which was annotated
 * with a {@link Scope} for which no corresponding {@link ScopeResolver} was provided.
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
