package hs.ddif.core.instantiation;

import hs.ddif.core.inject.injectable.Injectable;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.QualifiedType;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Manages {@link ScopeResolver}s.
 */
public class ScopeResolverManager {
  private static final ScopeResolver NULL_SCOPE_RESOLVER = new NullScopeResolver();

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
  }

  /**
   * Return the {@link ScopeResolver} for the given {@link Injectable}.
   *
   * @param injectable an {@link Injectable}, cannot be {@code null}
   * @return a {@link ScopeResolver}, never {@code null}
   */
  public ScopeResolver getScopeResolver(Injectable injectable) {
    Annotation scope = injectable.getScope();

    return scopeResolversByAnnotation.getOrDefault(scope == null ? null : scope.annotationType(), NULL_SCOPE_RESOLVER);
  }

  /**
   * Checks whether the given scope is known.
   *
   * @param scope a scope to check, cannot be {@code null}
   * @return {@code true} if the scope is known, otherwise {@code false}
   */
  public boolean isRegisteredScope(Class<? extends Annotation> scope) {
    return scopeResolversByAnnotation.containsKey(scope);
  }

  private static class NullScopeResolver implements ScopeResolver {
    @Override
    public Class<? extends Annotation> getScopeAnnotationClass() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isScopeActive(QualifiedType qualifiedType) {
      return true;
    }

    @Override
    public <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
      return objectFactory.call();
    }
  }
}
