package org.int4.dirk.spi.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.int4.dirk.api.scope.ScopeNotActiveException;

/**
 * Abstract base implementation of a {@link ScopeResolver} which manages a map of instances per scope.
 *
 * @param <S> the type of the scope discriminator object
 */
public abstract class AbstractScopeResolver<S> implements ScopeResolver {
  private final Map<S, Map<Object, CreationalContext<?>>> instancesByScope = new ConcurrentHashMap<>();

  @Override
  public final boolean isActive() {
    return getCurrentScope() != null;
  }

  @Override
  public final <T> T get(Object key, CreationalContext<T> creationalContext) throws Exception {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new ScopeNotActiveException("Scope not active: " + getAnnotation() + " for: " + key);
    }

    Map<Object, CreationalContext<?>> instances = instancesByScope.computeIfAbsent(currentScope, k -> new ConcurrentHashMap<>());

    @SuppressWarnings("unchecked")
    CreationalContext<T> existingContext = (CreationalContext<T>)instances.computeIfAbsent(key, k -> creationalContext);

    return existingContext.get();
  }

  @Override
  public final void remove(Object key) {
    for(Map<Object, CreationalContext<?>> map : instancesByScope.values()) {
      CreationalContext<?> existingContext = map.remove(key);

      if(existingContext != null) {
        existingContext.release();
      }
    }
  }

  /**
   * Returns the current scope, or {@code null} if there is no current scope.
   *
   * @return the current scope, or {@code null} if there is no current scope
   */
  protected abstract S getCurrentScope();

  /**
   * Destroys the given scope. Removes all references to instance from the given
   * scope and releases their associated {@link CreationalContext}s. Does nothing if the
   * scope does not exist.
   *
   * @param scope a scope, cannot be {@code null}
   */
  protected final void destroyScope(S scope) {
    Map<Object, CreationalContext<?>> instances = instancesByScope.remove(scope);

    if(instances != null) {
      for(CreationalContext<?> existingContext : instances.values()) {
        existingContext.release();
      }
    }
  }
}
