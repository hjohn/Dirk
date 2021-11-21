package hs.ddif.core.inject.store;

import hs.ddif.core.util.AnnotationDescriptor;
import hs.ddif.core.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Qualifier;
import javax.inject.Scope;

public class AnnotationExtractor {

  public static Annotation findScopeAnnotation(AnnotatedElement element) {
    List<Annotation> matchingAnnotations = AnnotationUtils.findAnnotations(element, Scope.class);

    if(matchingAnnotations.size() > 1) {
      throw new BindingException("Multiple scope annotations found, but only one allowed: " + element + ", found: " + matchingAnnotations);
    }

    return matchingAnnotations.isEmpty() ? null : matchingAnnotations.get(0);
  }

  public static Set<AnnotationDescriptor> extractQualifiers(AnnotatedElement element) {
    return extractQualifiers(element.getAnnotations());
  }

  private static Set<AnnotationDescriptor> extractQualifiers(Annotation[] annotations) {
    Set<AnnotationDescriptor> qualifiers = new HashSet<>();

    for(Annotation annotation : annotations) {
      if(annotation.annotationType().getAnnotation(Qualifier.class) != null) {
        qualifiers.add(new AnnotationDescriptor(annotation));
      }
    }

    return qualifiers;
  }
}
