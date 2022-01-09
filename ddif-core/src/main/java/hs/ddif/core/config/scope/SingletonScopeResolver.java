package hs.ddif.core.config.scope;

import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.QualifiedType;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import javax.inject.Singleton;

/**
 * Scope resolver for the {@link Singleton} scope.
 */
public class SingletonScopeResolver implements ScopeResolver {
  private final Map<QualifiedType, Object> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(QualifiedType qualifiedType) {
    return true;
  }

  @Override
  public <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
    @SuppressWarnings("unchecked")
    T singleton = (T)singletons.get(qualifiedType);

    if(singleton == null) {
      singleton = objectFactory.call();

      singletons.put(qualifiedType, singleton);
    }

    return singleton;
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return Singleton.class;
  }
}
