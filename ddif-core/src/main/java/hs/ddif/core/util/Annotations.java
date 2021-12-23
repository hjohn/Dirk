package hs.ddif.core.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.leangen.geantyref.TypeFactory;

/**
 * Utilities for working with annotations.
 */
public class Annotations {

  /**
   * Creates an {@link Annotation} with parameters.
   *
   * @param annotationClass a {@link Class} which extends {@link Annotation}, cannot be null
   * @param values a map of values for the annotations parameters, cannot be null
   * @return an {@link Annotation}, never null
   * @throws IllegalArgumentException when the given parameters could not be converted to an annotation
   */
  public static Annotation of(Class<? extends Annotation> annotationClass, Map<String, Object> values) {
    try {
      return TypeFactory.annotation(annotationClass, values);
    }
    catch(Exception e) {
      throw new IllegalArgumentException("Could not convert to annotation: " + annotationClass + " " + values, e);
    }
  }

  /**
   * Creates an {@link Annotation} without parameters.
   *
   * @param annotationClass a {@link Class} which extends {@link Annotation}, cannot be null
   * @return an {@link Annotation}, never null
   * @throws IllegalArgumentException when the given parameters could not be converted to an annotation
   */
  public static Annotation of(Class<? extends Annotation> annotationClass) {
    return of(annotationClass, Map.of());
  }

  /**
   * Finds all annotations of the given type recursively from the given {@link AnnotatedElement}.
   *
   * @param element an {@link AnnotatedElement}, cannot be null
   * @param annotationType a {@link Class} which extends {@link Annotation}, cannot be null
   * @return a set of {@link Annotation}s that were found directly or indirectly on the given element, never null and never contains nulls
   */
  public static Set<Annotation> findAnnotations(AnnotatedElement element, Class<? extends Annotation> annotationType) {
    Set<Annotation> matchingAnnotations = new HashSet<>();
    Deque<Annotation> annotations = new ArrayDeque<>();

    annotations.addAll(Arrays.asList(element.getAnnotations()));

    Set<Annotation> visited = new HashSet<>(annotations);

    while(!annotations.isEmpty()) {
      Annotation annotation = annotations.remove();

      for(Annotation childAnnotation : annotation.annotationType().getAnnotations()) {
        if(visited.add(childAnnotation)) {
          annotations.add(childAnnotation);
        }
      }

      if(annotation.annotationType() == annotationType) {
        matchingAnnotations.add(annotation);
      }
    }

    return matchingAnnotations;
  }

  /**
   * Recursively finds annotations <b>directly</b> annotated by a given meta annotation, starting
   * from the given {@link AnnotatedElement}. The annotations returned are not necessarily direct
   * annotations on the given element, but can be meta annotations which are annotated with the
   * given meta annotations.
   *
   * @param element an {@link AnnotatedElement}, cannot be null
   * @param metaAnnotation an {@link Annotation}, cannot be null
   * @return a set of {@link Annotation}s directly annotated with the given meta annotation, never null and never contains nulls
   */
  public static Set<Annotation> findDirectlyMetaAnnotatedAnnotations(AnnotatedElement element, Annotation metaAnnotation) {
    Set<Annotation> matchingAnnotations = new HashSet<>();
    Deque<Annotation> annotations = new ArrayDeque<>();

    annotations.addAll(Arrays.asList(element.getAnnotations()));

    Set<Annotation> visited = new HashSet<>(annotations);

    while(!annotations.isEmpty()) {
      Annotation annotation = annotations.remove();

      for(Annotation childAnnotation : annotation.annotationType().getAnnotations()) {
        if(visited.add(childAnnotation)) {
          annotations.add(childAnnotation);
        }
        if(childAnnotation.equals(metaAnnotation)) {
          matchingAnnotations.add(annotation);
        }
      }
    }

    return matchingAnnotations;
  }

  /**
   * Returns {@link Annotation}s on the given {@link AnnotatedElement} which are annotated with the given meta annotation
   * directly or indirectly.
   *
   * @param element an {@link AnnotatedElement}, cannot be null
   * @param metaAnnotation an {@link Annotation}, cannot be null
   * @return {@link Annotation}s on the given {@link AnnotatedElement} which are annotated with the given meta annotation
   *   directly or indirectly, never null and never contains nulls
   */
  public static List<Annotation> findMetaAnnotatedAnnotations(AnnotatedElement element, Annotation metaAnnotation) {
    return Stream.of(element.getAnnotations())
      .filter(annotation -> isMetaAnnotated(annotation, metaAnnotation))
      .collect(Collectors.toList());
  }

  /**
   * Returns {@code true} if the given {@link Annotation} is directly or indirectly meta
   * annotated with the given meta annotation.
   *
   * @param annotation an {@link Annotation} to check, cannot be null
   * @param metaAnnotation a meta {@link Annotation} to search for, cannot be null
   * @return {@code true} if the given {@link Annotation} is directly or indirectly meta
   *   annotated with the given meta annotation, otherwise {@code false}
   */
  public static boolean isMetaAnnotated(Annotation annotation, Annotation metaAnnotation) {
    return findAnnotations(annotation.annotationType(), metaAnnotation.annotationType()).contains(metaAnnotation);
  }
}
