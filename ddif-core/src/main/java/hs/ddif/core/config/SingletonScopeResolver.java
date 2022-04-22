package hs.ddif.core.config;

import hs.ddif.spi.scope.AbstractScopeResolver;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Scope resolver for singleton scope.
 */
public class SingletonScopeResolver extends AbstractScopeResolver<String> {
  private final Class<? extends Annotation> singletonAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param singleton a singleton annotation {@link Class} to use, cannot be {@code null}
   */
  public SingletonScopeResolver(Class<? extends Annotation> singleton) {
    this.singletonAnnotation = Objects.requireNonNull(singleton, "singleton cannot be null");
  }

  @Override
  public Class<? extends Annotation> getAnnotationClass() {
    return singletonAnnotation;
  }

  @Override
  protected String getCurrentScope() {
    return "singleton";
  }
}
