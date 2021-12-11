package hs.ddif.core.scope;

import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.store.Injectable;
import hs.ddif.core.util.InformationalWeakReference;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;

public class WeakSingletonScopeResolver implements ScopeResolver {
  private static final WeakReferenceCleanupLogger cleanupLogger = new WeakReferenceCleanupLogger();

  private final Map<Injectable, InformationalWeakReference<Object>> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(Injectable key) {
    return true;
  }

  @Override
  public <T> T get(Injectable key) {
    InformationalWeakReference<Object> reference = singletons.get(key);

    if(reference != null) {
      @SuppressWarnings("unchecked")
      T instance = (T)reference.get();

      return instance;  // This may still return null
    }

    return null;
  }

  @Override
  public <T> void put(Injectable key, T instance) {
    singletons.put(key, new InformationalWeakReference<>(instance, cleanupLogger.getReferenceQueue()));
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return WeakSingleton.class;
  }
}
