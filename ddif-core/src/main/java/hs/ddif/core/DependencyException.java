package hs.ddif.core;

/**
 * Base exception type for exceptions that can occur during registration
 * and removals of classes, providers and instances with injectors.
 */
public abstract class DependencyException extends RuntimeException {

  public DependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public DependencyException(String message) {
    super(message);
  }

}
