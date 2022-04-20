package hs.ddif.core.config;

import hs.ddif.api.annotation.AnnotationStrategy;
import hs.ddif.api.annotation.InjectorStrategy;
import hs.ddif.api.annotation.ProxyStrategy;
import hs.ddif.api.annotation.ScopeStrategy;

import java.util.Objects;

/**
 * Implementation of {@link InjectorStrategy}.
 */
public class DefaultInjectorStrategy implements InjectorStrategy {
  private final AnnotationStrategy annotationStrategy;
  private final ScopeStrategy scopeStrategy;
  private final ProxyStrategy proxyStrategy;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param scopeStrategy a {@link ScopeStrategy}, cannot be {@code null}
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   */
  public DefaultInjectorStrategy(AnnotationStrategy annotationStrategy, ScopeStrategy scopeStrategy, ProxyStrategy proxyStrategy) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.scopeStrategy = Objects.requireNonNull(scopeStrategy, "scopeStrategy");
    this.proxyStrategy = Objects.requireNonNull(proxyStrategy, "proxyStrategy");
  }

  @Override
  public AnnotationStrategy getAnnotationStrategy() {
    return annotationStrategy;
  }

  @Override
  public ScopeStrategy getScopeStrategy() {
    return scopeStrategy;
  }

  @Override
  public ProxyStrategy getProxyStrategy() {
    return proxyStrategy;
  }
}
