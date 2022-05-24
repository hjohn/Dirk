package org.int4.dirk.spi.scope;

/**
 * Thrown when an attempt is made to register an injectable which was annotated
 * with a scope for which no corresponding {@link org.int4.dirk.spi.scope.ScopeResolver} was provided.
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
