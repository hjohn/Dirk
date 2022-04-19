package hs.ddif.core.config;

import hs.ddif.api.annotation.AnnotationStrategy;
import hs.ddif.api.annotation.InjectorStrategy;
import hs.ddif.api.annotation.ScopeStrategy;

import java.util.Objects;

/**
 * Implementation of {@link InjectorStrategy}.
 */
public class DefaultInjectorStrategy implements InjectorStrategy {
  private final AnnotationStrategy annotationStrategy;
  private final ScopeStrategy scopeStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param scopeStrategy a {@link ScopeStrategy}, cannot be {@code null}
   */
  public DefaultInjectorStrategy(AnnotationStrategy annotationStrategy, ScopeStrategy scopeStrategy) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.scopeStrategy = Objects.requireNonNull(scopeStrategy, "scopeStrategy");
  }

  @Override
  public AnnotationStrategy getAnnotationStrategy() {
    return annotationStrategy;
  }

  @Override
  public ScopeStrategy getScopeStrategy() {
    return scopeStrategy;
  }
}
