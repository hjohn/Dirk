package hs.ddif.core.scope;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.config.scope.WeakSingletonScopeResolver;

import java.util.stream.Stream;

public class ScopeResolverManagers {

  public static ScopeResolverManager create(ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(), new WeakSingletonScopeResolver()};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);

    return new ScopeResolverManager(extendedScopeResolvers);
  }
}

