package org.int4.dirk.spi.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import org.int4.dirk.api.definition.DefinitionException;
import org.int4.dirk.spi.scope.ScopeResolver;

/**
 * Defines the strategy for scope handling.
 */
public interface ScopeStrategy {

  /**
   * Returns the annotation that marks the default scope. Objects without
   * a scope annotation will get this scope as their scope.
   *
   * @return an annotation, never {@code null}
   */
  Annotation getDefaultAnnotation();

  /**
   * Returns the annotation that marks the dependent pseudo-scope.
   *
   * @return an annotation, never {@code null}
   */
  Annotation getDependentAnnotation();

  /**
   * Returns the annotation that marks the singleton pseudo-scope.
   *
   * @return an annotation, never {@code null}
   */
  Annotation getSingletonAnnotation();

  /**
   * Returns the scope annotation on the given {@link AnnotatedElement}, if any. It
   * is recommended to return {@code null} instead of a default scope annotation if
   * no scope is found on the given element.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return an annotation, or {@code null} if the element was not annotated with a scope annotation
   * @throws DefinitionException when the strategy detects an annotation problem
   */
  Annotation getScope(AnnotatedElement element) throws DefinitionException;

  /**
   * Returns whether the given {@link ScopeResolver} is handling a pseudo-scope.
   *
   * @param scopeResolver a {@link ScopeResolver}, cannot be {@code null}
   * @return {@code true} if the given {@link ScopeResolver} is handling a pseudo-scope, otherwise {@code false}
   */
  boolean isPseudoScope(ScopeResolver scopeResolver);
}
