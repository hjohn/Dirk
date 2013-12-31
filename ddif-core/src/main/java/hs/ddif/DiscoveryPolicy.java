package hs.ddif;

import java.lang.reflect.Type;

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
}
