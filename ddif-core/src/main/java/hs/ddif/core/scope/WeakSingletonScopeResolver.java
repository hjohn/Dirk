package hs.ddif.core.scope;

import hs.ddif.annotations.WeakSingleton;
import hs.ddif.core.util.InformationalWeakReference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;

public class WeakSingletonScopeResolver implements ScopeResolver {
  private static final WeakReferenceCleanupLogger cleanupLogger = new WeakReferenceCleanupLogger();

  private final Map<Type, InformationalWeakReference<Object>> singletons = new WeakHashMap<>();

  @Override
  public boolean isScopeActive(Type injectableType) {
    return true;
  }

  @Override
  public <T> T get(Type injectableType) {
    InformationalWeakReference<Object> reference = singletons.get(injectableType);

    if(reference != null) {
      @SuppressWarnings("unchecked")
      T bean = (T)reference.get();

      return bean;  // This may still return null
    }

    return null;
  }

  @Override
  public <T> void put(Type injectableType, T instance) {
    singletons.put(injectableType, new InformationalWeakReference<>(instance, cleanupLogger.getReferenceQueue()));
  }

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return WeakSingleton.class;
  }
}
