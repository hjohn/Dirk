package hs.ddif.core.inject.consistency;

/**
 * Thrown during registration when an injectable is determined to be depedendant on
 * a bean with a narrower scope.
 */
public class ScopeConflictException extends InjectorStoreConsistencyException {

  public ScopeConflictException(String message) {
    super(message);
  }
}
