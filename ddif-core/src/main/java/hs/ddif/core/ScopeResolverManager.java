package hs.ddif.core;

import hs.ddif.spi.scope.CreationalContext;
import hs.ddif.spi.scope.ScopeResolver;
import hs.ddif.spi.scope.UnknownScopeException;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages {@link ScopeResolver}s.
 */
class ScopeResolverManager {

  /**
   * Map containing {@link ScopeResolver}s.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopeResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param scopeResolvers a list of {@link ScopeResolver}, cannot be {@code null} but can be empty
   * @param dependentAnnotationClass a dependent scope annotation {@link Class}, cannot be {@code null}
   */
  public ScopeResolverManager(List<ScopeResolver> scopeResolvers, Class<? extends Annotation> dependentAnnotationClass) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      if(scopeResolver.getAnnotationClass() == null) {
        throw new IllegalArgumentException("scopeResolvers cannot have a null annotation class: " + scopeResolver);
      }

      ScopeResolver resolver = scopeResolversByAnnotation.put(scopeResolver.getAnnotationClass(), scopeResolver);

      if(resolver != null) {
        throw new IllegalArgumentException("scopeResolvers should not contain multiple resolvers with the same annotation class: " + resolver.getAnnotationClass());
      }
    }

    scopeResolversByAnnotation.put(Objects.requireNonNull(dependentAnnotationClass, "dependentAnnotationClass"), new DependentScopeResolver(dependentAnnotationClass));
  }

  /**
   * Return the {@link ScopeResolver} for the given scope {@link Annotation}. The given
   * scope can be {@code null} in which case no scope is assumed.
   *
   * @param scope a scope {@link Annotation}, can be {@code null}
   * @return a {@link ScopeResolver}, never {@code null}
   */
  public ScopeResolver getScopeResolver(Class<? extends Annotation> scope) {
    ScopeResolver scopeResolver = scopeResolversByAnnotation.get(scope);

    if(scopeResolver == null) {
      throw new UnknownScopeException("Unknown scope encountered: " + scope);
    }

    return scopeResolver;
  }

  private static class DependentScopeResolver implements ScopeResolver {
    private final Class<? extends Annotation> dependentAnnotationClass;

    DependentScopeResolver(Class<? extends Annotation> dependentAnnotationClass) {
      this.dependentAnnotationClass = dependentAnnotationClass;
    }

    @Override
    public Class<? extends Annotation> getAnnotationClass() {
      return dependentAnnotationClass;
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
