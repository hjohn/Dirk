package hs.ddif.core.store;

import java.lang.reflect.Type;

/**
 * Interface to define ways to discover new {@link Injectable} objects when
 * not found by an {@link InjectableStore}.
 *
 * @param <T> the type of {@link Injectable}
 */
public interface DiscoveryPolicy<T extends Injectable> {

  /**
   * Called when the given type is not found by the store.
   *
   * @param injectableStore an {@link InjectableStore}
   * @param type the type which was not found by the store
   */
  void discoverType(InjectableStore<T> injectableStore, Type type);
}
