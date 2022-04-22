package hs.ddif.spi.scope;

import hs.ddif.spi.scope.CreationalContext.Reference;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base implementation of a {@link ScopeResolver} which manages a map of instances per scope.
 *
 * @param <S> the type of the scope discriminator object
 */
public abstract class AbstractScopeResolver<S> implements ScopeResolver {
  private final Map<S, Map<Object, Reference<?>>> instancesByScope = new HashMap<>();

  @Override
  public final boolean isActive() {
    return getCurrentScope() != null;
  }

  @Override
  public final <T> T get(Object key, CreationalContext<T> creationalContext) throws Exception {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(key, getAnnotationClass());
    }

    Map<Object, Reference<?>> instances = instancesByScope.computeIfAbsent(currentScope, k -> new HashMap<>());

    @SuppressWarnings("unchecked")
    Reference<T> reference = (Reference<T>)instances.get(key);

    if(reference == null) {
      reference = creationalContext.create();

      instances.put(key, reference);
    }

    return reference.get();
  }

  @Override
  public final void remove(Object key) {
    for(Map<Object, Reference<?>> map : instancesByScope.values()) {
      Reference<?> reference = map.remove(key);

      if(reference != null) {
        reference.release();
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
    Map<Object, Reference<?>> instances = instancesByScope.remove(scope);

    if(instances != null) {
      for(Reference<?> reference : instances.values()) {
        reference.release();
      }
    }
  }
}
