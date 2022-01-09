package hs.ddif.plugins;

import hs.ddif.annotations.PluginScoped;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;
import hs.ddif.core.store.QualifiedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A {@link ScopeResolver} to support the {@link PluginScoped} scope.
 */
public class PluginScopeResolver implements ScopeResolver {
  private final Map<Plugin, Map<QualifiedType, Object>> instancesByScope = new HashMap<>();
  private final Map<Type, Plugin> typeToPlugin = new HashMap<>();

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return PluginScoped.class;
  }

  @Override
  public boolean isScopeActive(QualifiedType key) {
    return getCurrentScope(key) != null;
  }

  @Override
  public synchronized <T> T get(QualifiedType qualifiedType, Callable<T> objectFactory) throws Exception {
    Plugin currentScope = getCurrentScope(qualifiedType);

    if(currentScope == null) {
      throw new OutOfScopeException(qualifiedType, getScopeAnnotationClass());
    }

    @SuppressWarnings("unchecked")
    T instance = (T)instancesByScope.getOrDefault(currentScope, Collections.emptyMap()).get(qualifiedType);

    if(instance == null) {
      instance = objectFactory.call();

      instancesByScope.computeIfAbsent(currentScope, k -> new HashMap<>()).put(qualifiedType, instance);
    }

    return instance;
  }

  synchronized void register(Plugin plugin) {
    for(Type type : plugin.getTypes()) {
      Plugin p = typeToPlugin.get(type);

      if(p != null) {
        throw new IllegalStateException("Plugin " + p + " already registered type: " + type);
      }
    }

    for(Type type : plugin.getTypes()) {
      typeToPlugin.put(type, plugin);
    }
  }

  synchronized void unregister(Plugin plugin) {
    for(Type type : plugin.getTypes()) {
      Plugin p = typeToPlugin.get(type);

      if(p == null) {
        throw new IllegalStateException("Plugin " + plugin + " never registered type: " + type);
      }
      if(!p.equals(plugin)) {
        throw new IllegalStateException("Plugin " + p + " registered type " + type + ", but does not match: " + plugin);
      }
    }

    for(Type type : plugin.getTypes()) {
      typeToPlugin.remove(type);
    }

    instancesByScope.remove(plugin);
  }

  private Plugin getCurrentScope(QualifiedType qualifiedType) {
    return typeToPlugin.get(qualifiedType.getType());
  }
}
