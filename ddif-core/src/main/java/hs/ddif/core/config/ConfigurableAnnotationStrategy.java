package hs.ddif.core.config;

import hs.ddif.api.annotation.AnnotationStrategy;
import hs.ddif.api.definition.DefinitionException;
import hs.ddif.api.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Comparator;
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
  private final List<Class<? extends Annotation>> injectAnnotations;
  private final List<Class<? extends Annotation>> qualifierAnnotations;
  private final List<Class<? extends Annotation>> scopeAnnotations;
  private final List<Class<? extends Annotation>> optionalAnnotations;

  /**
   * Constructs a new instance.
   *
   * @param injectAnnotations a list of inject annotation {@link Class}es, cannot be {@code null}, contain {@code null}s or be empty
   * @param qualifierAnnotations a list of qualifier annotation {@link Class}es, cannot be {@code null}, contain {@code null}s or be empty
   * @param scopeAnnotations a list of scope annotation {@link Class}es, cannot be {@code null}, contain {@code null}s or be empty
   * @param optionalAnnotations a list of optional annotation {@link Class}es, cannot be {@code null}, contain {@code null}s but can be empty
   */
  public ConfigurableAnnotationStrategy(List<Class<? extends Annotation>> injectAnnotations, List<Class<? extends Annotation>> qualifierAnnotations, List<Class<? extends Annotation>> scopeAnnotations, List<Class<? extends Annotation>> optionalAnnotations) {
    if(injectAnnotations == null || injectAnnotations.isEmpty()) {
      throw new IllegalArgumentException("injectAnnotations cannot be null or empty: " + injectAnnotations);
    }
    if(qualifierAnnotations == null || qualifierAnnotations.isEmpty()) {
      throw new IllegalArgumentException("qualifierAnnotations cannot be null or empty: " + injectAnnotations);
    }
    if(scopeAnnotations == null || scopeAnnotations.isEmpty()) {
      throw new IllegalArgumentException("scopeAnnotations cannot be null or empty: " + injectAnnotations);
    }
    if(optionalAnnotations == null) {
      throw new IllegalArgumentException("optionalAnnotations cannot be null");
    }

    this.injectAnnotations = new ArrayList<>(injectAnnotations);
    this.qualifierAnnotations = new ArrayList<>(qualifierAnnotations);
    this.scopeAnnotations = new ArrayList<>(scopeAnnotations);
    this.optionalAnnotations = new ArrayList<>(optionalAnnotations);
  }

  /**
   * Constructs a new instance.
   *
   * @param inject an inject annotation {@link Class} to use, cannot be {@code null}
   * @param qualifier a qualifier annotation {@link Class} to use, cannot be {@code null}
   * @param scope a scope annotation {@link Class} to use, cannot be {@code null}
   * @param optional an optional annotation {@link Class} to use, can be {@code null}
   */
  public ConfigurableAnnotationStrategy(Class<? extends Annotation> inject, Class<? extends Annotation> qualifier, Class<? extends Annotation> scope, Class<? extends Annotation> optional) {
    this.injectAnnotations = List.of(Objects.requireNonNull(inject, "inject cannot be null"));
    this.qualifierAnnotations = List.of(Objects.requireNonNull(qualifier, "qualifier cannot be null"));
    this.scopeAnnotations = List.of(Objects.requireNonNull(scope, "scope cannot be null"));
    this.optionalAnnotations = optional == null ? List.of() : List.of(optional);
  }

  @Override
  public boolean isOptional(AnnotatedElement element) {
    if(element != null) {
      for(Annotation annotation : element.getAnnotations()) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String simpleName = annotationType.getName();

        if(simpleName.endsWith(".Nullable") || optionalAnnotations.contains(annotationType)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public Set<Annotation> getInjectAnnotations(AnnotatedElement element) throws DefinitionException {
    return getAnnotations(element).injects;
  }

  @Override
  public Set<Annotation> getQualifiers(AnnotatedElement element) {
    return getAnnotations(qualifierAnnotations, element);
  }

  @Override
  public Annotation getScope(AnnotatedElement element) {
    Set<Annotation> scopes = getAnnotations(scopeAnnotations, element);

    if(scopes.size() > 1) {
      throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
    }

    if(scopes.isEmpty()) {
      return null;
    }

    return scopes.iterator().next();
  }

  @Override
  public boolean isQualifier(Annotation annotation) {
    return isAnnotated(qualifierAnnotations, annotation.annotationType());
  }

  private DiscoveredAnnotations getAnnotations(AnnotatedElement element) throws DefinitionException {
    return new DiscoveredAnnotations(element);
  }

  private static boolean isAnnotated(List<Class<? extends Annotation>> annotations, AnnotatedElement element) {
    for(int i = 0, size = annotations.size(); i < size; i++) {
      if(element.isAnnotationPresent(annotations.get(i))) {
        return true;
      }
    }

    return false;
  }

  private static Set<Annotation> getAnnotations(List<Class<? extends Annotation>> annotations, AnnotatedElement element) {
    Set<Annotation> matchingAnnotations = new HashSet<>();

    for(int i = 0, size = annotations.size(); i < size; i++) {
      matchingAnnotations.addAll(Annotations.findDirectlyMetaAnnotatedAnnotations(element, annotations.get(i)));
    }

    return matchingAnnotations;
  }

  private class DiscoveredAnnotations {
    final Set<Annotation> injects;
    final Set<Annotation> scopes;

    DiscoveredAnnotations(AnnotatedElement element) throws DefinitionException {
      this.scopes = getAnnotations(scopeAnnotations, element);

      if(scopes.size() > 1) {
        throw new DefinitionException(element, "cannot have multiple scope annotations, but found: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }

      this.injects = Stream.of(element.getAnnotations()).filter(a -> injectAnnotations.contains(a.annotationType())).collect(Collectors.toSet());

      if(!scopes.isEmpty() && !injects.isEmpty()) {
        throw new DefinitionException(element, "cannot have both inject and scope annotations, but found: " + injects.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()) + " and: " + scopes.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList()));
      }
    }
  }
}
