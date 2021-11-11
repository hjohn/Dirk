package hs.ddif.core.scope;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Abstract base implementation of a {@link ScopeResolver} which manages a map of beans per scope.  Scopes
 * are weak referenced, and if the scope is no longer strongly referenced it and all its beans will become
 * eligble for garbage collection.
 *
 * @param <S> the type of the scope discriminator object
 */
public abstract class AbstractScopeResolver<S> implements ScopeResolver {
  private final Map<S, Map<Type, Object>> beansByScope = new WeakHashMap<>();

  /**
   * Returns the current scope, or <code>null</code> if there is no current scope.
   *
   * @return the current scope, or <code>null</code> if there is no current scope
   */
  public abstract S getCurrentScope();

  @Override
  public boolean isScopeActive(Type injectableType) {
    return getCurrentScope() != null;
  }

  @Override
  public <T> T get(Type injectableType) throws OutOfScopeException {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(injectableType, getScopeAnnotationClass());
    }

    Map<Type, Object> beans = beansByScope.get(currentScope);

    if(beans != null) {
      @SuppressWarnings("unchecked")
      T bean = (T)beans.get(injectableType);

      return bean;  // This may still return null
    }

    return null;
  }

  @Override
  public <T> void put(Type injectableType, T instance) throws OutOfScopeException {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(injectableType, getScopeAnnotationClass());
    }

    Map<Type, Object> beans = beansByScope.get(currentScope);

    if(beans == null) {
      beans = new WeakHashMap<>();
      beansByScope.put(currentScope, beans);
    }

    beans.put(injectableType, instance);
  }

  protected void clear() {
    beansByScope.clear();
  }
}
