package hs.ddif;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Interface to define ways to discover new {@link Injectable} objects when
 * not found by an {@link InjectableStore}.
 */
public interface DiscoveryPolicy {

  /**
   * Called when the given type is not found by the store.
   *
   * @param injectableStore an {@link InjectableStore}
   * @param type the type which was not found by the store
   */
  void discoverType(InjectableStore injectableStore, Type type);

  /**
   * Called when an {@link Injectable} object's dependencies may need to be
   * discovered before being added to the store.
   *
   * @param injectableStore an {@link InjectableStore}
   * @param injectable the {@link Injectable} being added
   * @param bindings the bindings of the {@link Injectable}
   */
  void discoverDependencies(InjectableStore injectableStore, Injectable injectable, Map<AccessibleObject, Binding> bindings);
}
