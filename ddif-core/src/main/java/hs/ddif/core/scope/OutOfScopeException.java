package hs.ddif.core.scope;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class OutOfScopeException extends Exception {

  public OutOfScopeException(Injectable key, Class<? extends Annotation> scopeAnnotationClass) {
    super("Scope not active: " + scopeAnnotationClass + " for key: " + key);
  }
}
