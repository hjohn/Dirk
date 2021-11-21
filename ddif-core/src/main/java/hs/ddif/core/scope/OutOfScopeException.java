package hs.ddif.core.scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class OutOfScopeException extends Exception {

  public OutOfScopeException(Type type, Class<? extends Annotation> scopeAnnotationClass) {
    super("Scope not active: " + scopeAnnotationClass + " for type: " + type);
  }
}
