package hs.ddif.core;

public abstract class DependencyException extends RuntimeException {

  public DependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public DependencyException(String message) {
    super(message);
  }

}
