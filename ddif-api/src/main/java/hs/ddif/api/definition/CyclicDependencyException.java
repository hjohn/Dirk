package hs.ddif.api.definition;

import java.util.Objects;

/**
 * Thrown when a type or a group of types which are being registered has a
 * dependency which directly or indirectly refers to the type containing the
 * dependency, otherwise known as a cyclical dependency.
 *
 * <p>If the cyclical dependency cannot be resolved (by means of proxies for
 * example) the injector can throw this exception to indicate the types involved.
 * If this exception is thrown, the problem can be solved by the user by breaking
 * the cyclical dependency or by using a provider for one of the dependencies in
 * the cycle.
 */
public class CyclicDependencyException extends DependencyException {

  /**
   * Constructs a new instance.
   *
   * @param message a message, cannot be {@code null}
   */
  public CyclicDependencyException(String message) {
    super(Objects.requireNonNull(message, "message"));
  }
}
