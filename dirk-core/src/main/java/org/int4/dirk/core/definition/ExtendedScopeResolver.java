package org.int4.dirk.core.definition;

import java.lang.annotation.Annotation;
import java.util.Objects;

import org.int4.dirk.api.scope.ScopeNotActiveException;
import org.int4.dirk.spi.scope.CreationalContext;
import org.int4.dirk.spi.scope.ScopeResolver;

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
  public Annotation getAnnotation() {
    return delegate.getAnnotation();
  }

  @Override
  public boolean isActive() {
    return delegate.isActive();
  }

  @Override
  public <T> T get(Object key, CreationalContext<T> creationalContext) throws ScopeNotActiveException, Exception {
    return delegate.get(key, creationalContext);
  }

  @Override
  public void remove(Object key) {
    delegate.remove(key);
  }
}
