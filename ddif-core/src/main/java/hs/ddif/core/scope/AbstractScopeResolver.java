package hs.ddif.core.scope;

import hs.ddif.core.instantiation.injection.Constructable;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base implementation of a {@link ScopeResolver} which manages a map of instances per scope.
 *
 * @param <S> the type of the scope discriminator object
 */
public abstract class AbstractScopeResolver<S> implements ScopeResolver {
  private final Map<S, Map<Constructable<?>, ContextualInstance<?>>> instancesByScope = new HashMap<>();

  @Override
  public final boolean isScopeActive() {
    return getCurrentScope() != null;
  }

  @Override
  public final <T> T get(Constructable<T> constructable, InjectionContext injectionContext) throws Exception {
    S currentScope = getCurrentScope();

    if(currentScope == null) {
      throw new OutOfScopeException(constructable, getScopeAnnotationClass());
    }

    Map<Constructable<?>, ContextualInstance<?>> instances = instancesByScope.computeIfAbsent(currentScope, k -> new HashMap<>());

    @SuppressWarnings("unchecked")
    ContextualInstance<T> contextualInstance = (ContextualInstance<T>)instances.get(constructable);
    T instance = contextualInstance == null ? null : contextualInstance.instance;

    if(instance == null) {
      instance = constructable.create(injectionContext.getInjections());

      instances.put(constructable, new ContextualInstance<>(instance, injectionContext));
    }

    return instance;
  }

  @Override
  public final <T> void remove(Constructable<T> constructable) {
    for(Map<Constructable<?>, ContextualInstance<?>> map : instancesByScope.values()) {
      ContextualInstance<?> contextualInstance = map.remove(constructable);

      if(contextualInstance != null) {
        contextualInstance.injectionContext.release();
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
   * scope and releases their associated {@link InjectionContext}s. Does nothing if the
   * scope does not exist.
   *
   * @param scope a scope, cannot be {@code null}
   */
  protected final void destroyScope(S scope) {
    Map<Constructable<?>, ContextualInstance<?>> instances = instancesByScope.remove(scope);

    if(instances != null) {
      for(ContextualInstance<?> contextualInstance : instances.values()) {
        contextualInstance.injectionContext.release();
      }
    }
  }

  private static class ContextualInstance<T> {
    final T instance;
    final InjectionContext injectionContext;

    ContextualInstance(T instance, InjectionContext injectionContext) {
      this.instance = instance;
      this.injectionContext = injectionContext;
    }
  }
}
