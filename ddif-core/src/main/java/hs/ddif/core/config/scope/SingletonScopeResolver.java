package hs.ddif.core.config.scope;

import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import javax.inject.Singleton;

/**
 * Scope resolver for the {@link Singleton} scope.
 */
public class SingletonScopeResolver implements ScopeResolver {
  private final Map<Object, Object> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive() {
    return true;
  }

  @Override
  public <T> T get(Object key, Callable<T> objectFactory) throws Exception {
    @SuppressWarnings("unchecked")
    T singleton = (T)singletons.get(key);

    if(singleton == null) {
      singleton = objectFactory.call();

      singletons.put(key, singleton);
    }

    return singleton;
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return Singleton.class;
  }

  @Override
  public void remove(Object object) {
    singletons.remove(object);
  }
}
