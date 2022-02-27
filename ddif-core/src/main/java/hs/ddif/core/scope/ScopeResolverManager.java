package hs.ddif.core.scope;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Manages {@link ScopeResolver}s.
 */
public class ScopeResolverManager {

  /**
   * Map containing {@link ScopeResolver}s.
   */
  private final Map<Class<? extends Annotation>, ScopeResolver> scopeResolversByAnnotation = new HashMap<>();

  /**
   * Constructs a new instance.
   *
   * @param scopeResolvers an array of {@link ScopeResolver}, cannot be {@code null} but can be empty
   */
  public ScopeResolverManager(ScopeResolver... scopeResolvers) {
    for(ScopeResolver scopeResolver : scopeResolvers) {
      scopeResolversByAnnotation.put(scopeResolver.getScopeAnnotationClass(), scopeResolver);
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
    public Class<? extends Annotation> getScopeAnnotationClass() {
      return null;
    }

    @Override
    public boolean isScopeActive() {
      return true;
    }

    @Override
    public <T> T get(Object key, Callable<T> objectFactory) throws Exception {
      return objectFactory.call();
    }

    @Override
    public void remove(Object object) {
      // does nothing
    }
  }
}
