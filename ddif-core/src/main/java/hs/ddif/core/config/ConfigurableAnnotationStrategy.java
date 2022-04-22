package hs.ddif.core.config;

import hs.ddif.api.util.Annotations;
import hs.ddif.spi.config.AnnotationStrategy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link AnnotationStrategy} which allows configuring one or more
 * annotations used to control injection.
 */
public class ConfigurableAnnotationStrategy implements AnnotationStrategy {
  private final Class<? extends Annotation> injectAnnotation;
  private final Class<? extends Annotation> qualifierAnnotation;
  private final Class<? extends Annotation> optionalAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param inject an inject annotation {@link Class} to use, cannot be {@code null}
   * @param qualifier a qualifier annotation {@link Class} to use, cannot be {@code null}
   * @param optional an optional annotation {@link Class} to use, can be {@code null}
   */
  public ConfigurableAnnotationStrategy(Class<? extends Annotation> inject, Class<? extends Annotation> qualifier, Class<? extends Annotation> optional) {
    this.injectAnnotation = Objects.requireNonNull(inject, "inject");
    this.qualifierAnnotation = Objects.requireNonNull(qualifier, "qualifier");
    this.optionalAnnotation = optional;
  }

  @Override
  public boolean isOptional(AnnotatedElement element) {
    if(element != null) {
      for(Annotation annotation : element.getAnnotations()) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String simpleName = annotationType.getName();

        if(simpleName.endsWith(".Nullable") || optionalAnnotation == annotationType) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public Set<Annotation> getInjectAnnotations(AnnotatedElement element) {
    return Annotations.findAnnotations(element, injectAnnotation);
  }

  @Override
  public Set<Annotation> getQualifiers(AnnotatedElement element) {
    return Annotations.findDirectlyMetaAnnotatedAnnotations(element, qualifierAnnotation);
  }

  @Override
  public boolean isQualifier(Annotation annotation) {
    return annotation.annotationType().isAnnotationPresent(qualifierAnnotation);
  }
}
