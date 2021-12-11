package hs.ddif.core.scope;

import hs.ddif.core.store.Injectable;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Singleton;

public class SingletonScopeResolver implements ScopeResolver {
  private final Map<Injectable, Object> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(Injectable key) {
    return true;
  }

  @Override
  public <T> T get(Injectable key) {
    @SuppressWarnings("unchecked")
    T singleton = (T)singletons.get(key);

    return singleton;
  }

  @Override
  public <T> void put(Injectable key, T instance) {
    singletons.put(key, instance);
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return Singleton.class;
  }
}
