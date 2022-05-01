package hs.ddif.api.definition;

/**
 * Thrown during registration when a type is determined to dependent on a normal
 * scoped type of a different scope than the type being registered, and the injector
 * is not able or configured to resolve the problem itself (for example, by using
 * a proxy).
 *
 * <p>A dependency resulting in a scope conflict can be wrapped in a provider to
 * resolve this problem. When called, the provider may still fail at run time if
 * the scope of the dependency is not active.
 */
public class ScopeConflictException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message
   * @param cause a cause
   */
  public ScopeConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
