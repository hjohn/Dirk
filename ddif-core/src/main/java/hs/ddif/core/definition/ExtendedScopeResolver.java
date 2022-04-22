package hs.ddif.core.definition;

import hs.ddif.spi.scope.CreationalContext;
import hs.ddif.spi.scope.OutOfScopeException;
import hs.ddif.spi.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * Wrapper for {@link ScopeResolver} with additional information useful for the internal
 * workings of the injector framework.
 */
public class ExtendedScopeResolver implements ScopeResolver {
  private final ScopeResolver delegate;
  private final boolean isPseudoScope;
  private final boolean isDependentScope;

  /**
   * Constructs a new instance.
   *
   * @param delegate a delegate {@link ScopeResolver}, cannot be {@code null}
   * @param isPseudoScope whether the delegate represents a pseudo-scope
   * @param isDependentScope whether the delegate represents the dependent scope
   */
  public ExtendedScopeResolver(ScopeResolver delegate, boolean isPseudoScope, boolean isDependentScope) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.isPseudoScope = isPseudoScope;
    this.isDependentScope = isDependentScope;
  }

  /**
   * Returns whether this {@link ScopeResolver} represents a pseudo-scope.
   *
   * @return {@code true} if it represents a pseudo-scope, otherwise {@code false}
   */
  public boolean isPseudoScope() {
    return isPseudoScope;
  }

  /**
   * Returns whether this {@link ScopeResolver} represents the dependent pseudo-scope.
   *
   * @return {@code true} if it represents the dependent pseudo-scope, otherwise {@code false}
   */
  public boolean isDependentScope() {
    return isDependentScope;
  }

  @Override
  public Class<? extends Annotation> getAnnotationClass() {
    return delegate.getAnnotationClass();
  }

  @Override
  public boolean isActive() {
    return delegate.isActive();
  }

  @Override
  public <T> T get(Object key, CreationalContext<T> creationalContext) throws OutOfScopeException, Exception {
    return delegate.get(key, creationalContext);
  }

  @Override
  public void remove(Object key) {
    delegate.remove(key);
  }
}
