package hs.ddif.core.scope;

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
  private final Map<S, Map<Class<?>, Object>> beansByScope = new WeakHashMap<>();

  /**
   * Returns the current scope, or <code>null</code> if there is no current scope.
   *
   * @return the current scope, or <code>null</code> if there is no current scope
   */
  public abstract S getCurrentScope();

  @Override
  public <T> T get(Class<?> injectableClass) {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException("No scope active for: " + getScopeAnnotationClass());
    }

    Map<Class<?>, Object> beans = beansByScope.get(currentScope);

    if(beans != null) {
      @SuppressWarnings("unchecked")
      T bean = (T)beans.get(injectableClass);

      return bean;  // This may still return null
    }

    return null;
  }

  @Override
  public <T> void put(Class<?> injectableClass, T instance) {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException("No scope active for: " + getScopeAnnotationClass());
    }

    Map<Class<?>, Object> beans = beansByScope.get(currentScope);

    if(beans == null) {
      beans = new WeakHashMap<>();
      beansByScope.put(currentScope, beans);
    }

    beans.put(injectableClass, instance);
  }

  protected void clear() {
    beansByScope.clear();
  }
}
