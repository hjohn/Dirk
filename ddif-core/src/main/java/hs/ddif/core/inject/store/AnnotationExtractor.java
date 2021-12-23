package hs.ddif.core.inject.store;

import hs.ddif.core.util.Annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import javax.inject.Scope;

class AnnotationExtractor {
  private static final Annotation SCOPE = Annotations.of(Scope.class);

  static Annotation findScopeAnnotation(AnnotatedElement element) {
    Set<Annotation> matchingAnnotations = Annotations.findDirectlyMetaAnnotatedAnnotations(element, SCOPE);

    if(matchingAnnotations.size() > 1) {
      throw new BindingException("Multiple scope annotations found, but only one allowed: " + element + ", found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.iterator().next();
  }
}
