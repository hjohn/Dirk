package hs.ddif.core;

import hs.ddif.core.config.SingletonScopeResolver;
import hs.ddif.core.test.scope.Dependent;
import hs.ddif.spi.scope.ScopeResolver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Singleton;

public class ScopeResolverManagers {

  public static ScopeResolverManager create(ScopeResolver... scopeResolvers) {
    ScopeResolver[] standardScopeResolvers = new ScopeResolver[] {new SingletonScopeResolver(Singleton.class)};
    List<ScopeResolver> extendedScopeResolvers = Stream.of(scopeResolvers, standardScopeResolvers).flatMap(Stream::of).collect(Collectors.toList());

    return new ScopeResolverManager(extendedScopeResolvers, Dependent.class);
  }
}

