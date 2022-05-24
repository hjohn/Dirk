package org.int4.dirk.library;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Set;

import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.util.Annotations;

/**
 * Implementation of {@link AnnotationStrategy} which allows configuring one or more
 * annotations used to control injection.
 */
public class ConfigurableAnnotationStrategy implements AnnotationStrategy {
  private final Class<? extends Annotation> injectClass;
  private final Class<? extends Annotation> qualifierClass;
  private final Class<? extends Annotation> optionalClass;
  private final Annotation qualifierAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param inject an inject annotation {@link Class} to use, cannot be {@code null}
   * @param qualifier a qualifier annotation {@link Class} to use, cannot be {@code null}
   * @param optional an optional annotation {@link Class} to use, can be {@code null}
   */
  public ConfigurableAnnotationStrategy(Class<? extends Annotation> inject, Class<? extends Annotation> qualifier, Class<? extends Annotation> optional) {
    this.injectClass = Objects.requireNonNull(inject, "inject");
    this.qualifierClass = Objects.requireNonNull(qualifier, "qualifier");
    this.optionalClass = optional;
    this.qualifierAnnotation = Annotations.of(qualifierClass);
  }

  @Override
  public boolean isOptional(AnnotatedElement element) {
    if(element != null) {
      for(Annotation annotation : element.getAnnotations()) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String simpleName = annotationType.getName();

        if(simpleName.endsWith(".Nullable") || optionalClass == annotationType) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public Set<Annotation> getInjectAnnotations(AnnotatedElement element) {
    return Annotations.findAnnotations(element, injectClass);
  }

  @Override
  public Set<Annotation> getQualifiers(AnnotatedElement element) {
    return Annotations.findDirectlyMetaAnnotatedAnnotations(element, qualifierClass);
  }

  @Override
  public boolean isQualifier(Annotation annotation) {
    return Annotations.isMetaAnnotated(annotation.annotationType(), qualifierAnnotation);
  }
}
