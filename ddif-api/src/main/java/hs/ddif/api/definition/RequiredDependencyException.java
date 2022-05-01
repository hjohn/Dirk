package hs.ddif.api.definition;

/**
 * Thrown when an attempt is made to register or remove a type that would cause a
 * dependency required by another type to be ambiguous or unresolvable (either
 * by providing a second alternative or by not providing one anymore).<p>
 *
 * For example, when a type exists that requires a Database implementation and
 * the only Database implementation is removed, then its requirements can no
 * longer be met.
 */
public abstract class RequiredDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public RequiredDependencyException(String message) {
    super(message);
  }
}
