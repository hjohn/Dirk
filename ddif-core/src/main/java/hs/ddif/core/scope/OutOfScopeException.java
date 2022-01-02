package hs.ddif.core.scope;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class OutOfScopeException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param key an {@link Injectable} serving as the key, cannot be null
   * @param scopeAnnotationClass the class of the scope annotation involved, cannot be null
   */
  public OutOfScopeException(Injectable key, Class<? extends Annotation> scopeAnnotationClass) {
    super("Scope not active: " + scopeAnnotationClass + " for key: " + key);
  }
}
