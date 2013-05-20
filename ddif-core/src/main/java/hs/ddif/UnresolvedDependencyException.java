package hs.ddif;

public class UnresolvedDependencyException extends DependencyException {

  public UnresolvedDependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnresolvedDependencyException(String message) {
    super(message);
  }

}
