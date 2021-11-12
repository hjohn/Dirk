package hs.ddif.core.scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Singleton;

public class SingletonScopeResolver implements ScopeResolver {
  private final Map<Type, Object> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(Type injectableType) {
    return true;
  }

  @Override
  public <T> T get(Type injectableType) {
    @SuppressWarnings("unchecked")
    T singleton = (T)singletons.get(injectableType);

    return singleton;
  }

  @Override
  public <T> void put(Type injectableType, T instance) {
    singletons.put(injectableType, instance);
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return Singleton.class;
  }
}
