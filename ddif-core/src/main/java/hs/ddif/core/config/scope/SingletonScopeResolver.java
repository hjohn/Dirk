package hs.ddif.core.config.scope;

import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

/**
 * Scope resolver for singleton scope.
 */
public class SingletonScopeResolver implements ScopeResolver {
  private final Map<Object, Object> singletons = new WeakHashMap<>();
  private final Class<? extends Annotation> singletonAnnotation;

  /**
   * Constructs a new instance.
   *
   * @param singleton a singleton {@link Annotation} to use, cannot be {@code null}
   */
  public SingletonScopeResolver(Annotation singleton) {
    this.singletonAnnotation = Objects.requireNonNull(singleton, "singleton cannot be null").annotationType();
  }

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
    return singletonAnnotation;
  }

  @Override
  public void remove(Object object) {
    singletons.remove(object);
  }

  @Override
  public boolean isSingletonScope() {
    return true;
  }
}
