package hs.ddif.spi.config;

/**
 * Strategy for controlling the behavior of an injector.
 */
public interface InjectorStrategy {

  /**
   * Returns the {@link AnnotationStrategy} to be used.
   *
   * @return the {@link AnnotationStrategy}, never {@code null}
   */
  AnnotationStrategy getAnnotationStrategy();

  /**
   * Returns the {@link ScopeStrategy} to be used.
   *
   * @return the {@link ScopeStrategy}, never {@code null}
   */
  ScopeStrategy getScopeStrategy();

  /**
   * Returns the {@link ProxyStrategy} to be used.
   *
   * @return the {@link ProxyStrategy}, never {@code null}
   */
  ProxyStrategy getProxyStrategy();

  /**
   * Returns the {@link LifeCycleCallbacksFactory} to be used.
   *
   * @return a {@link LifeCycleCallbacksFactory}, never {@code null}
   */
  LifeCycleCallbacksFactory getLifeCycleCallbacksFactory();
}
