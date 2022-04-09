package hs.ddif.api.scope;

import java.lang.annotation.Annotation;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class OutOfScopeException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param key an {@link Object}, cannot be {@code null}
   * @param scopeAnnotationClass the class of the scope annotation involved, cannot be {@code null}
   */
  public OutOfScopeException(Object key, Class<? extends Annotation> scopeAnnotationClass) {
    super("Scope not active: " + scopeAnnotationClass + " for: " + key);
  }
}
