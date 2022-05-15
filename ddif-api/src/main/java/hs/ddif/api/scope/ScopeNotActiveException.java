package hs.ddif.api.scope;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class ScopeNotActiveException extends ScopeException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public ScopeNotActiveException(String message) {
    super(message);
  }
}
