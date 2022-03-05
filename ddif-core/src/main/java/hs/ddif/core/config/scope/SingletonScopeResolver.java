package hs.ddif.core.config.scope;

import hs.ddif.core.scope.AbstractScopeResolver;

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
   * @param singleton a singleton {@link Annotation} to use, cannot be {@code null}
   */
  public SingletonScopeResolver(Annotation singleton) {
    this.singletonAnnotation = Objects.requireNonNull(singleton, "singleton cannot be null").annotationType();
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return singletonAnnotation;
  }

  @Override
  protected String getCurrentScope() {
    return "singleton";
  }

  @Override
  public boolean isSingletonScope() {
    return true;
  }
}
