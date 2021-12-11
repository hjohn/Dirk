package hs.ddif.core;

import hs.ddif.core.scope.ScopeResolver;

/**
 * Factory for {@link Injector}s of various types.
 */
public class Injectors {

  /**
   * Creates an {@link Injector} with auto discovery activated and the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never null
   */
  public static Injector autoDiscovering(ScopeResolver... scopeResolvers) {
    return new Injector(true, scopeResolvers);
  }

  /**
   * Creates an {@link Injector} which must be manually configured with the given
   * {@link ScopeResolver}s.
   *
   * @param scopeResolvers an optional array of {@link ScopeResolver}s
   * @return an {@link Injector}, never null
   */
  public static Injector manual(ScopeResolver... scopeResolvers) {
    return new Injector(false, scopeResolvers);
  }
}
