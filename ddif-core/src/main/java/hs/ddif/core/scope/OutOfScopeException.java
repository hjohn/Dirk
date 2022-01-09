package hs.ddif.core.scope;

import hs.ddif.core.store.QualifiedType;

import java.lang.annotation.Annotation;

/**
 * Thrown when a scoped instance is required without the appropriate scope being active.
 */
public class OutOfScopeException extends Exception {

  /**
   * Constructs a new instance.
   *
   * @param qualifiedType a {@link QualifiedType}, cannot be {@code null}
   * @param scopeAnnotationClass the class of the scope annotation involved, cannot be {@code null}
   */
  public OutOfScopeException(QualifiedType qualifiedType, Class<? extends Annotation> scopeAnnotationClass) {
    super("Scope not active: " + scopeAnnotationClass + " for: " + qualifiedType);
  }
}
