package hs.ddif.core.definition.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

/**
 * Strategy for checking and obtaining annotations relevant to dependency injection.
 */
public interface AnnotationStrategy {

  /**
   * Returns all {@link Annotation}s on the given {@link AnnotatedElement} which
   * are inject annotations.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return a modifiable set of {@link Annotation}s, never {@code null} or contains {@code null}, but can be empty
   */
  Set<Annotation> getInjectAnnotations(AnnotatedElement element);

  /**
   * Checks if the given {@link AnnotatedElement} is annotated by an inject annotation.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return {@code true} if the given {@link AnnotatedElement} is annotated by an inject annotation, otherwise {@code false}
   */
  boolean isInjectAnnotated(AnnotatedElement element);

  /**
   * Returns all {@link Annotation}s on the given {@link AnnotatedElement} which
   * are meta annotated by a qualifier annotation.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return a modifiable set of {@link Annotation}s, never {@code null} or contains {@code null}, but can be empty
   */
  Set<Annotation> getQualifiers(AnnotatedElement element);

  /**
   * Returns all {@link Annotation}s on the given {@link AnnotatedElement} which
   * are meta annotated by a scope annotation.
   *
   * @param element an {@link AnnotatedElement}, cannot be {@code null}
   * @return a modifiable set of {@link Annotation}s, never {@code null} or contains {@code null}, but can be empty
   */
  Set<Annotation> getScopes(AnnotatedElement element);

  /**
   * Checks if the given {@link Annotation} is a qualifier annotation. This means it
   * is meta annotated by one of the configured qualifier annotations.
   *
   * @param annotation an {@link Annotation}, cannot be {@code null}
   * @return {@code true} if the given annotation is a qualifier annotation, otherwise {@code false}
   */
  boolean isQualifier(Annotation annotation);
}
