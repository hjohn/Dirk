package hs.ddif.core;

import hs.ddif.api.scope.CreationalContext;
import hs.ddif.api.scope.ScopeResolver;
import hs.ddif.api.scope.UnknownScopeException;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   */
  public ScopeResolverManager(List<ScopeResolver> scopeResolvers) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      if(scopeResolver.getAnnotationClass() == null) {
        throw new IllegalArgumentException("scopeResolvers cannot have a null annotation class: " + scopeResolver);
      }

      ScopeResolver resolver = scopeResolversByAnnotation.put(scopeResolver.getAnnotationClass(), scopeResolver);

      if(resolver != null) {
        throw new IllegalArgumentException("scopeResolvers should not contain multiple resolvers with the same annotation class: " + resolver.getAnnotationClass());
      }
    }

    scopeResolversByAnnotation.put(null, new NullScopeResolver());
  }

  /**
   * Return the {@link ScopeResolver} for the given scope {@link Annotation}. The given
   * scope can be {@code null} in which case no scope is assumed.
   *
   * @param scope a scope {@link Annotation}, can be {@code null}
   * @return a {@link ScopeResolver}, never {@code null}
   */
  public ScopeResolver getScopeResolver(Annotation scope) {
    ScopeResolver scopeResolver = scopeResolversByAnnotation.get(scope == null ? null : scope.annotationType());

    if(scopeResolver == null) {
      throw new UnknownScopeException("Unknown scope encountered: " + scope);
    }

    return scopeResolver;
  }

  private static class NullScopeResolver implements ScopeResolver {
    @Override
    public Class<? extends Annotation> getAnnotationClass() {
      return null;
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
