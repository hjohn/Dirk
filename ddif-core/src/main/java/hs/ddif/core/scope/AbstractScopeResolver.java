package hs.ddif.core.scope;

import hs.ddif.core.store.QualifiedType;

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
  private final Map<S, Map<QualifiedType, Object>> instancesByScope = new WeakHashMap<>();

  /**
   * Returns the current scope, or <code>null</code> if there is no current scope.
   *
   * @return the current scope, or <code>null</code> if there is no current scope
   */
  public abstract S getCurrentScope();

  @Override
  public boolean isScopeActive(QualifiedType qualifiedType) {
    return getCurrentScope() != null;
  }

  @Override
  public <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(qualifiedType, getScopeAnnotationClass());
    }

    Map<QualifiedType, Object> instances = instancesByScope.computeIfAbsent(currentScope, k -> new WeakHashMap<>());

    @SuppressWarnings("unchecked")
    T instance = (T)instances.get(qualifiedType);

    if(instance == null) {
      instance = objectFactory.call();

      instances.put(qualifiedType, instance);
    }

    return instance;
  }

  /**
   * Removes all instances from the resolver.
   */
  protected void clear() {
    instancesByScope.clear();
  }
}
