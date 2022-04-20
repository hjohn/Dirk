package hs.ddif.api.annotation;

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
}
