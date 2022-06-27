package org.int4.dirk.extensions.assisted;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;

/**
 * Annotation strategy for {@link AssistedTypeRegistrationExtension}. This strategy
 * configures which marker annotation triggers generation of the assisted producer
 * implementation, which argument annotation can be used to indicate arguments,
 * and how to extract argument names from fields, methods and parameters.
 *
 * <p>The strategy also must supply the inject annotation in order for the extension
 * to create an assisted producer implementation that can be injected.
 */
public interface AssistedAnnotationStrategy {

  /**
   * Returns the {@link Class} of the marker annotation to indicate a producer
   * should be provided by the assisted injection extension.
   *
   * @return an annotation {@link Class} for the assisted annotation, never {@code null}
   */
  Class<? extends Annotation> assistedAnnotationClass();

  /**
   * Checks if the given {@link AnnotatedElement} is annotated as an argument.
   *
   * @param annotatedElement an {@link AnnotatedElement}, cannot be {@code null}
   * @return {@code true} if the element was annotated as an argument, otherwise {@code false}
   */
  boolean isArgument(AnnotatedElement annotatedElement);

  /**
   * Determines the name of the argument given an argument annotation and an {@link AccessibleObject}.
   * The {@link AccessibleObject} provided is either a method or a field.
   *
   * <p>Returning {@code null}, although allowed, is considered a fatal problem and
   * will result in an exception indicating the argument name must be provided somehow.
   *
   * @param accessibleObject an {@link AccessibleObject}, cannot be {@code null}
   * @return an argument name, never {@code null}
   * @throws MissingArgumentException when argument name could not be determined
   */
  String determineArgumentName(AccessibleObject accessibleObject) throws MissingArgumentException;

  /**
   * Determines the name of the argument given an argument annotation and a {@link Parameter}.
   *
   * <p>Returning {@code null}, although allowed, is considered a fatal problem and
   * will result in an exception indicating the argument name must be provided somehow.
   *
   * @param parameter a {@link Parameter}, cannot be {@code null}
   * @return an argument name, never {@code null}
   * @throws MissingArgumentException when argument name could not be determined
   */
  String determineArgumentName(Parameter parameter) throws MissingArgumentException;

  /**
   * Returns the inject annotation supported by the associated injector.
   *
   * @return an inject annotation supported by the associated injector, never {@code null}
   */
  Annotation injectAnnotation();
}
