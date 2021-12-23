package hs.ddif.core.inject.store;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Qualifier;
import javax.inject.Scope;

class AnnotationExtractor {
  private static final Annotation QUALIFIER = Annotations.of(Qualifier.class);
  private static final Annotation SCOPE = Annotations.of(Scope.class);

  static Annotation findScopeAnnotation(AnnotatedElement element) {
    Set<Annotation> matchingAnnotations = Annotations.findDirectlyMetaAnnotatedAnnotations(element, SCOPE);

    if(matchingAnnotations.size() > 1) {
      throw new BindingException("Multiple scope annotations found, but only one allowed: " + element + ", found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.iterator().next();
  }

  static Set<AnnotationDescriptor> extractQualifiers(AnnotatedElement element) {
    return Annotations.findDirectlyMetaAnnotatedAnnotations(element, QUALIFIER).stream()
      .map(AnnotationDescriptor::new)
      .collect(Collectors.toSet());
  }
}
