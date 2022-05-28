package org.int4.dirk.core;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.int4.dirk.spi.scope.CreationalContext;
import org.int4.dirk.spi.scope.ScopeResolver;
import org.int4.dirk.spi.scope.UnknownScopeException;

/**
 * Manages {@link ScopeResolver}s.
 */
class ScopeResolverManager {

  /**
   * Map containing {@link ScopeResolver}s.
   */
  private final Map<Annotation, ScopeResolver> scopeResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param scopeResolvers a list of {@link ScopeResolver}, cannot be {@code null} but can be empty
   * @param dependentAnnotation a dependent scope annotation, cannot be {@code null}
   */
  public ScopeResolverManager(List<ScopeResolver> scopeResolvers, Annotation dependentAnnotation) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      if(scopeResolver.getAnnotation() == null) {
        throw new IllegalArgumentException("scopeResolvers cannot have a null annotation: " + scopeResolver);
      }

      ScopeResolver resolver = scopeResolversByAnnotation.put(scopeResolver.getAnnotation(), scopeResolver);

      if(resolver != null) {
        throw new IllegalArgumentException("scopeResolvers should not contain multiple resolvers with the same annotation: " + resolver.getAnnotation());
      }
    }

    scopeResolversByAnnotation.put(Objects.requireNonNull(dependentAnnotation, "dependentAnnotationClass"), new DependentScopeResolver(dependentAnnotation));
  }

  /**
   * Return the {@link ScopeResolver} for the given scope {@link Annotation}. The given
   * scope can be {@code null} in which case no scope is assumed.
   *
   * @param scope a scope {@link Annotation}, can be {@code null}
   * @return a {@link ScopeResolver}, never {@code null}
   */
  public ScopeResolver getScopeResolver(Annotation scope) {
    ScopeResolver scopeResolver = scopeResolversByAnnotation.get(scope);

    if(scopeResolver == null) {
      throw new UnknownScopeException("Unknown scope encountered: " + scope);
    }

    return scopeResolver;
  }

  private static class DependentScopeResolver implements ScopeResolver {
    private final Annotation dependentAnnotation;

    DependentScopeResolver(Annotation dependentAnnotation) {
      this.dependentAnnotation = dependentAnnotation;
    }

    @Override
    public Annotation getAnnotation() {
      return dependentAnnotation;
    }

    @Override
    public boolean isActive() {
      return true;
    }

    @Override
    public <T> T get(Object key, CreationalContext<T> creationalContext) throws Exception {
      return creationalContext.create().get();
    }

    @Override
    public void remove(Object key) {
      // does nothing
    }
  }
}
