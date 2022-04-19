package hs.ddif.api.annotation;

import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Defines the strategy for scope handling.
 */
public interface ScopeStrategy {

  /**
   * Returns the scope annotation on the given {@link AnnotatedElement}, if any.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return an annotation {@link Class}, or {@code null} if the element was not annotated with a scope annotation
   * @throws DefinitionException when the strategy detects an annotation problem
   */
  Class<? extends Annotation> getScope(AnnotatedElement element) throws DefinitionException;

}
