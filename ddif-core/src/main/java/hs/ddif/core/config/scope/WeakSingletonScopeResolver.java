package hs.ddif.core.config.scope;

import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.QualifiedType;
import hs.ddif.core.util.InformationalWeakReference;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

/**
 * Scope resolver for the {@link WeakSingleton} scope.
 */
public class WeakSingletonScopeResolver implements ScopeResolver {
  private static final WeakReferenceCleanupLogger cleanupLogger = new WeakReferenceCleanupLogger();

  private final Map<QualifiedType, InformationalWeakReference<Object>> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(QualifiedType qualifiedType) {
    return true;
  }

  @Override
  public <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
    InformationalWeakReference<Object> reference = singletons.get(qualifiedType);

    @SuppressWarnings("unchecked")
    T instance = reference == null ? null : (T)reference.get();

    if(instance == null) {
      instance = objectFactory.call();

      singletons.put(qualifiedType, new InformationalWeakReference<>(instance, cleanupLogger.getReferenceQueue()));
    }

    return instance;
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return WeakSingleton.class;
  }
}