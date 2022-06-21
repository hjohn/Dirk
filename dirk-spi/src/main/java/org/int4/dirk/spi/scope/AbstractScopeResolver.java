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
  public final CreationalContext<?> find(Object key) {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new ScopeNotActiveException("Scope not active: " + getAnnotation() + " for: " + key);
    }

    Map<Object, CreationalContext<?>> map = instancesByScope.get(currentScope);

    return map == null ? null : map.get(key);
  }

  @Override
  public final void put(Object key, CreationalContext<?> creationalContext) {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new ScopeNotActiveException("Scope not active: " + getAnnotation() + " for: " + key);
    }

    instancesByScope.computeIfAbsent(currentScope, k -> new ConcurrentHashMap<>()).put(key, creationalContext);
  }

  @Override
  public final void remove(Object key) {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new ScopeNotActiveException("Scope not active: " + getAnnotation() + " for: " + key);
    }

    Map<Object, CreationalContext<?>> map = instancesByScope.get(currentScope);

    if(map != null) {
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
