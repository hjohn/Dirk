package hs.ddif.core;

/**
 * Thrown when an injectable is determined to be depedendant on a bean with a narrower scope.
 */
public class ScopeConflictException extends DependencyException {

  public ScopeConflictException(String message) {
    super(message);
  }
}
