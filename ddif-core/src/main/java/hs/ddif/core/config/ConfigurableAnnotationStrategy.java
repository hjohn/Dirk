package hs.ddif.core.config;

import hs.ddif.core.definition.bind.AnnotationStrategy;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link AnnotationStrategy} which allows configuring one or more
 * annotations used to control injection.
 */
public class ConfigurableAnnotationStrategy implements AnnotationStrategy {
  private final List<Annotation> injectAnnotations;
  private final List<Annotation> qualifierAnnotations;
  private final List<Annotation> scopeAnnotations;

  /**
   * Constructs a new instance.
   *
   * @param injectAnnotations a list of inject {@link Annotation}s, cannot be {@code null}, contain {@code null}s or be empty
   * @param qualifierAnnotations a list of qualifier {@link Annotation}s, cannot be {@code null}, contain {@code null}s or be empty
   * @param scopeAnnotations a list of scope {@link Annotation}s, cannot be {@code null}, contain {@code null}s or be empty
   */
  public ConfigurableAnnotationStrategy(List<Annotation> injectAnnotations, List<Annotation> qualifierAnnotations, List<Annotation> scopeAnnotations) {
    if(injectAnnotations == null || injectAnnotations.isEmpty()) {
      throw new IllegalArgumentException("injectAnnotations cannot be null or empty: " + injectAnnotations);
    }
    if(qualifierAnnotations == null || qualifierAnnotations.isEmpty()) {
      throw new IllegalArgumentException("qualifierAnnotations cannot be null or empty: " + injectAnnotations);
    }
    if(scopeAnnotations == null || scopeAnnotations.isEmpty()) {
      throw new IllegalArgumentException("scopeAnnotations cannot be null or empty: " + injectAnnotations);
    }

    this.injectAnnotations = new ArrayList<>(injectAnnotations);
    this.qualifierAnnotations = new ArrayList<>(qualifierAnnotations);
    this.scopeAnnotations = new ArrayList<>(scopeAnnotations);
  }

  /**
   * Constructs a new instance.
   *
   * @param inject an inject {@link Annotation} to use, cannot be {@code null}
   * @param qualifier a qualifier {@link Annotation} to use, cannot be {@code null}
   * @param scope a scope {@link Annotation} to use, cannot be {@code null}
   */
  public ConfigurableAnnotationStrategy(Annotation inject, Annotation qualifier, Annotation scope) {
    this.injectAnnotations = List.of(Objects.requireNonNull(inject, "inject cannot be null"));
    this.qualifierAnnotations = List.of(Objects.requireNonNull(qualifier, "qualifier cannot be null"));
    this.scopeAnnotations = List.of(Objects.requireNonNull(scope, "scope cannot be null"));
  }

  @Override
  public boolean isInjectAnnotated(AnnotatedElement element) {
    return isAnnotated(injectAnnotations, element);
  }

  @Override
  public Set<Annotation> getInjectAnnotations(AnnotatedElement element) {
    return Stream.of(element.getAnnotations()).filter(injectAnnotations::contains).collect(Collectors.toSet());
  }

  @Override
  public Set<Annotation> getQualifiers(AnnotatedElement element) {
    return getAnnotations(qualifierAnnotations, element);
  }

  @Override
  public Set<Annotation> getScopes(AnnotatedElement element) {
    return getAnnotations(scopeAnnotations, element);
  }

  @Override
  public boolean isQualifier(Annotation annotation) {
    return isAnnotated(qualifierAnnotations, annotation.annotationType());
  }

  private static boolean isAnnotated(List<Annotation> annotations, AnnotatedElement element) {
    for(int i = 0, size = annotations.size(); i < size; i++) {
      if(element.isAnnotationPresent(annotations.get(i).annotationType())) {
        return true;
      }
    }

    return false;
  }

  private static Set<Annotation> getAnnotations(List<Annotation> annotations, AnnotatedElement element) {
    Set<Annotation> matchingAnnotations = new HashSet<>();

    for(int i = 0, size = annotations.size(); i < size; i++) {
      matchingAnnotations.addAll(Annotations.findDirectlyMetaAnnotatedAnnotations(element, annotations.get(i)));
    }

    return matchingAnnotations;
  }
}
