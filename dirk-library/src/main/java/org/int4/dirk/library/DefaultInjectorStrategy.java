package org.int4.dirk.library;

import java.util.Objects;

import org.int4.dirk.spi.config.AnnotationStrategy;
import org.int4.dirk.spi.config.InjectorStrategy;
import org.int4.dirk.spi.config.LifeCycleCallbacksFactory;
import org.int4.dirk.spi.config.ProxyStrategy;
import org.int4.dirk.spi.config.ScopeStrategy;

/**
 * Implementation of {@link InjectorStrategy}.
 */
public class DefaultInjectorStrategy implements InjectorStrategy {
  private final AnnotationStrategy annotationStrategy;
  private final ScopeStrategy scopeStrategy;
  private final ProxyStrategy proxyStrategy;
  private final LifeCycleCallbacksFactory lifeCycleCallbacksFactory;

  /**
   * Constructs a new instance.
   *
   * @param annotationStrategy an {@link AnnotationStrategy}, cannot be {@code null}
   * @param scopeStrategy a {@link ScopeStrategy}, cannot be {@code null}
   * @param proxyStrategy a {@link ProxyStrategy}, cannot be {@code null}
   * @param lifeCycleCallbacksFactory a {@link LifeCycleCallbacksFactory}, cannot be {@code null}
   */
  public DefaultInjectorStrategy(AnnotationStrategy annotationStrategy, ScopeStrategy scopeStrategy, ProxyStrategy proxyStrategy, LifeCycleCallbacksFactory lifeCycleCallbacksFactory) {
    this.annotationStrategy = Objects.requireNonNull(annotationStrategy, "annotationStrategy");
    this.scopeStrategy = Objects.requireNonNull(scopeStrategy, "scopeStrategy");
    this.proxyStrategy = Objects.requireNonNull(proxyStrategy, "proxyStrategy");
    this.lifeCycleCallbacksFactory = Objects.requireNonNull(lifeCycleCallbacksFactory, "lifeCycleCallbacksFactory");
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

  @Override
  public LifeCycleCallbacksFactory getLifeCycleCallbacksFactory() {
    return lifeCycleCallbacksFactory;
  }
}
