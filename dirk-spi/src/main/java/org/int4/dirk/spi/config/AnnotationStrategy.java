package org.int4.dirk.spi.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import org.int4.dirk.api.definition.DefinitionException;

/**
 * Strategy for checking and obtaining annotations relevant to dependency injection.
 *
 * <p>Strategies can either resolve conflicting annotations or report them as a definition
 * problem. It is recommended the problems are reported.
 */
public interface AnnotationStrategy {

  /**
   * Returns all {@link Annotation}s on the given {@link AnnotatedElement} which
   * are inject annotations.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return a modifiable set of {@link Annotation}s, never {@code null} or contains {@code null}, but can be empty
   * @throws DefinitionException when the strategy detects an annotation problem
   */
  Set<Annotation> getInjectAnnotations(AnnotatedElement element) throws DefinitionException;

  /**
   * Checks if the given {@link AnnotatedElement} is optional. If optional and there is no
   * matching dependency, the injector will not thrown an exception but instead will skip
   * injecting fields and will provide {@code null} for parameters. This allows having default
   * values for field injection.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return {@code true} if the given {@link AnnotatedElement} is optional, otherwise {@code false}
   * @throws DefinitionException when the strategy detects an annotation problem
   */
  boolean isOptional(AnnotatedElement element) throws DefinitionException;

  /**
   * Returns all {@link Annotation}s on the given {@link AnnotatedElement} which
   * are qualifier annotations.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return a modifiable set of {@link Annotation}s, never {@code null} or contains {@code null}, but can be empty
   * @throws DefinitionException when the strategy detects an annotation problem
   */
  Set<Annotation> getQualifiers(AnnotatedElement element) throws DefinitionException;

  /**
   * Checks if the given {@link Annotation} is a qualifier annotation.
   *
   * @param annotation an {@link Annotation}, cannot be {@code null}
   * @return {@code true} if the given annotation is a qualifier annotation, otherwise {@code false}
   */
  boolean isQualifier(Annotation annotation);
}
