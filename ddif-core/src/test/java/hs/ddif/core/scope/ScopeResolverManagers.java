package hs.ddif.core.scope;

import hs.ddif.core.config.scope.SingletonScopeResolver;
import hs.ddif.core.util.Annotations;

import java.util.stream.Stream;

import jakarta.inject.Singleton;

public class ScopeResolverManagers {

  public static ScopeResolverManager create(ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(Annotations.of(Singleton.class))};
    ScopeResolver[] extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).toArray(ScopeResolver[]::new);

    return new ScopeResolverManager(extendedScopeResolvers);
  }
}

