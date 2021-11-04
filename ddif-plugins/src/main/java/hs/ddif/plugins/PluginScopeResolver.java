package hs.ddif.plugins;

import hs.ddif.annotations.PluginScoped;
import hs.ddif.core.scope.OutOfScopeException;
import hs.ddif.core.scope.ScopeResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ScopeResolver} to support the {@link PluginScoped} scope.
 */
public class PluginScopeResolver implements ScopeResolver {
  private final Map<Plugin, Map<Type, Object>> beansByScope = new HashMap<>();
  private final Map<Type, Plugin> typeToPlugin = new HashMap<>();

  @Override
  public Class<? extends Annotation> getScopeAnnotationClass() {
    return PluginScoped.class;
  }

  @Override
  public synchronized <T> T get(Type injectableType) throws OutOfScopeException {
    Plugin currentScope = getCurrentScope(injectableType);

    if(currentScope == null) {
      throw new OutOfScopeException(injectableType, getScopeAnnotationClass());
    }

    @SuppressWarnings("unchecked")
    T bean = (T)beansByScope.getOrDefault(currentScope, Collections.emptyMap()).get(injectableType);

    return bean;
  }

  @Override
  public synchronized <T> void put(Type injectableType, T instance) throws OutOfScopeException {
    Plugin currentScope = getCurrentScope(injectableType);

    if(currentScope == null) {
      throw new OutOfScopeException(injectableType, getScopeAnnotationClass());
    }

    beansByScope.computeIfAbsent(currentScope, k -> new HashMap<>()).put(injectableType, instance);
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

    beansByScope.remove(plugin);
  }

  private Plugin getCurrentScope(Type injectableType) {
    return typeToPlugin.get(injectableType);
  }
}
