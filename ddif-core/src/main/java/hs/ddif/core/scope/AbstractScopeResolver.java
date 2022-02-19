package hs.ddif.core.scope;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

/**
 * Abstract base implementation of a {@link ScopeResolver} which manages a map of instances per scope.  Scopes
 * are weak referenced, and if the scope is no longer strongly referenced it and all its instances will become
 * eligible for garbage collection.
 *
 * @param <S> the type of the scope discriminator object
 */
public abstract class AbstractScopeResolver<S> implements ScopeResolver {
  private final Map<S, Map<Object, Object>> instancesByScope = new WeakHashMap<>();

  /**
   * Returns the current scope, or <code>null</code> if there is no current scope.
   *
   * @return the current scope, or <code>null</code> if there is no current scope
   */
  public abstract S getCurrentScope();

  @Override
  public boolean isScopeActive() {
    return getCurrentScope() != null;
  }

  @Override
  public <T> T get(Object key, Callable<T> objectFactory) throws Exception {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(key, getScopeAnnotationClass());
    }

    Map<Object, Object> instances = instancesByScope.computeIfAbsent(currentScope, k -> new WeakHashMap<>());

    @SuppressWarnings("unchecked")
    T instance = (T)instances.get(key);

    if(instance == null) {
      instance = objectFactory.call();

      instances.put(key, instance);
    }

    return instance;
  }

  @Override
  public void remove(Object object) {
    for(Map<Object, Object> map : instancesByScope.values()) {
      map.remove(object);
    }
  }

  /**
   * Removes all instances from the resolver.
   */
  protected void clear() {
    instancesByScope.clear();
  }
}
